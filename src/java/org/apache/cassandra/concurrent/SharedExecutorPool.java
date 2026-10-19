/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.concurrent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Stream;

import org.apache.cassandra.concurrent.DebuggableTask.DebuggableTaskRunner;

import static org.apache.cassandra.concurrent.ExecutorFactory.Global.executorFactory;
import static org.apache.cassandra.concurrent.SEPWorker.Work;
import static org.apache.cassandra.utils.Clock.Global.nanoTime;

/**
 * A pool of worker threads that are shared between all Executors created with it. Each executor is treated as a distinct
 * unit, with its own concurrency and task queue limits, but the threads that service the tasks on each executor are
 * free to hop between executors at will.
 *
 * To keep producers from incurring unnecessary delays, once an executor is "spun up" (i.e. is processing tasks at a steady
 * rate), adding tasks to the executor often involves only placing the task on the work queue and updating the
 * task permits (which imposes our max queue length constraints). Only when it cannot be guaranteed the task will be serviced
 * promptly, and the maximum concurrency has not been reached, does the producer have to schedule a thread itself to perform
 * the work ('promptly' in this context means we already have a worker spinning for work, as described next).
 *
 * Otherwise the worker threads schedule themselves: when they are assigned a task, they will attempt to spawn
 * a partner worker to service any other work outstanding on the queue (if any); once they have finished the task they
 * will either take another (if any remaining) and repeat this, or they will attempt to assign themselves to another executor
 * that does have tasks remaining. If both fail, it will enter a non-busy-spinning phase, where it will sleep for a short
 * random interval (based upon the number of threads in this mode, so that the total amount of non-sleeping time remains
 * approximately fixed regardless of the number of spinning threads), and upon waking will again try to assign itself to
 * an executor with outstanding tasks to perform. As a result of always scheduling a partner before committing to performing
 * any work, with a steady state of task arrival we should generally have either one spinning worker ready to promptly respond
 * to incoming work, or all possible workers actively committed to tasks.
 *
 * In order to prevent this executor pool acting like a noisy neighbour to other processes on the system, workers also deschedule
 * themselves when it is detected that there are too many for the current rate of operation arrival. This is decided as a function
 * of the total time spent spinning by all workers in an interval; as more workers spin, workers are descheduled more rapidly.
 */
public class SharedExecutorPool
{
    public static final SharedExecutorPool SHARED = new SharedExecutorPool("SharedPool");

    // the name assigned to workers in the pool, and the id suffix
    final ThreadGroup threadGroup;
    final AtomicLong workerId = new AtomicLong();

    // the collection of executors serviced by this pool; periodically ordered by traffic volume
    public final List<SEPExecutor> executors = new CopyOnWriteArrayList<>();

    // the number of workers currently in a spinning state
    final AtomicInteger spinningCount = new AtomicInteger();
    // see SEPWorker.maybeStop() - used to self coordinate stopping of threads
    final AtomicLong stopCheck = new AtomicLong();

    /**
     * The number of workers that may register themselves as spinning at the same time. Each worker keeps its slot
     * for its whole lifetime, so a slot is only ever written by its owner and by the threads that claim its spin.
     */
    static final int MAX_SPINNING_WORKERS = 64;
    // the slots currently owned by a worker, as a bit mask; workers without a slot do not register their spins
    final AtomicLong spinningSlots = new AtomicLong();
    // the collection of threads that are (most likely) in a spinning state - new workers are scheduled from here first
    final AtomicReferenceArray<SEPWorker> spinners = new AtomicReferenceArray<>(MAX_SPINNING_WORKERS);
    // the collection of threads that have been asked to stop/deschedule - new workers are scheduled from here last
    final ConcurrentSkipListMap<Long, SEPWorker> descheduled = new ConcurrentSkipListMap<>();
    // All SEPWorkers that are currently running
    private final Set<SEPWorker> allWorkers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    volatile boolean shuttingDown = false;

    public SharedExecutorPool(String name)
    {
        this(executorFactory().newThreadGroup(name));
    }

    public SharedExecutorPool(ThreadGroup threadGroup)
    {
        this.threadGroup = threadGroup;
    }

    /**
     * Takes an exclusive spinning slot for a new worker, or -1 if every slot is already owned.
     */
    int acquireSpinningSlot()
    {
        while (true)
        {
            long mask = spinningSlots.get();
            if (mask == -1L)
                return -1;
            int slot = Long.numberOfTrailingZeros(~mask);
            if (spinningSlots.compareAndSet(mask, mask | (1L << slot)))
                return slot;
        }
    }

    /**
     * Releases a spinning slot, which must no longer be registered.
     */
    void releaseSpinningSlot(int slot)
    {
        if (slot < 0)
            return;
        spinners.set(slot, null);
        spinningSlots.updateAndGet(mask -> mask & ~(1L << slot));
    }

    /**
     * Takes the registered spinning worker whose scheduled wake-up is the earliest, if any is still spinning.
     * The claimed worker is unregistered, exactly as removing it from the previous spinning collection did, so that
     * it does not expect to be handed any further work by this mechanism.
     */
    private SEPWorker pollSpinningWorker()
    {
        // a claim can lose against the worker waking up and unregistering itself, in which case we look again
        for (int attempt = 0; attempt < 4; attempt++)
        {
            long mask = spinningSlots.get();
            if (mask == 0)
                return null;

            SEPWorker earliest = null;
            long earliestDeadline = Long.MAX_VALUE;
            for (long remaining = mask; remaining != 0; remaining &= remaining - 1)
            {
                SEPWorker worker = spinners.get(Long.numberOfTrailingZeros(remaining));
                if (worker == null)
                    continue;
                long deadline = worker.spinDeadline();
                if (deadline <= 0 || deadline >= earliestDeadline)
                    continue;
                earliest = worker;
                earliestDeadline = deadline;
            }

            if (earliest == null)
                return null;
            if (earliest.claimSpin(earliestDeadline))
                return earliest;
        }
        return null;
    }

    void schedule(Work work)
    {
        // we try to hand-off our work to the spinning queue before the descheduled queue, even though we expect it to be empty
        // all we're doing here is hoping to find a worker without work to do, but it doesn't matter too much what we find;
        // we atomically set the task so even if this were a collection of all workers it would be safe, and if they are both
        // empty we schedule a new thread
        SEPWorker spinner;
        while (null != (spinner = pollSpinningWorker()))
            if (spinner.assign(work, false))
                return;

        Map.Entry<Long, SEPWorker> e;
        while (null != (e = descheduled.pollFirstEntry()))
            if (e.getValue().assign(work, false))
                return;

        if (!work.isStop())
        {
            SEPWorker worker = new SEPWorker(threadGroup, workerId.incrementAndGet(), work, this);
            allWorkers.add(worker);
        }
    }

    void workerEnded(SEPWorker worker)
    {
        allWorkers.remove(worker);
        releaseSpinningSlot(worker.spinningSlot);
    }

    public Stream<? extends DebuggableTaskRunner> workers()
    {
        return allWorkers.stream().flatMap(worker -> Stream.of(worker, worker.immediateRunner()));
    }

    void maybeStartSpinningWorker()
    {
        // in general the workers manage spinningCount directly; however if it is zero, we increment it atomically
        // ourselves to avoid starting a worker unless we have to
        int current = spinningCount.get();
        if (current == 0 && spinningCount.compareAndSet(0, 1))
            schedule(Work.SPINNING);
    }

    public synchronized LocalAwareExecutorPlus newExecutor(int maxConcurrency, String jmxPath, String name)
    {
        return newExecutor(maxConcurrency, i -> {}, jmxPath, name);
    }

    public LocalAwareExecutorPlus newExecutor(int maxConcurrency, ExecutorPlus.MaximumPoolSizeListener maximumPoolSizeListener, String jmxPath, String name)
    {
        SEPExecutor executor = new SEPExecutor(this, maxConcurrency, maximumPoolSizeListener, jmxPath, name);
        executors.add(executor);
        return executor;
    }

    public synchronized void shutdownAndWait(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException
    {
        shuttingDown = true;
        for (SEPExecutor executor : executors)
            executor.shutdownNow();

        terminateWorkers();

        long until = nanoTime() + unit.toNanos(timeout);
        for (SEPExecutor executor : executors)
        {
            executor.shutdown.await(until - nanoTime(), TimeUnit.NANOSECONDS);
            if (!executor.isTerminated())
                throw new TimeoutException(executor.name + " not terminated");
        }
    }

    void terminateWorkers()
    {
        assert shuttingDown;

        // To terminate our workers, we only need to unpark thread to make it runnable again,
        // so that the pool.shuttingDown boolean is checked. If work was already in the process
        // of being scheduled, worker will terminate upon running the task.
        Map.Entry<Long, SEPWorker> e;
        while (null != (e = descheduled.pollFirstEntry()))
            e.getValue().assign(Work.SPINNING, false);

        long mask = spinningSlots.get();
        for (long remaining = mask; remaining != 0; remaining &= remaining - 1)
        {
            SEPWorker worker = spinners.get(Long.numberOfTrailingZeros(remaining));
            if (worker != null)
                LockSupport.unpark(worker.thread);
        }
    }
}
