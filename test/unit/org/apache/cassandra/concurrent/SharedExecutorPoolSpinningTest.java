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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.cassandra.config.DatabaseDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the allocation-free spinning-worker registry: slots are exclusive and released when a worker exits, and a
 * registered spinning worker is claimed to service work instead of the pool starting another worker.
 */
public class SharedExecutorPoolSpinningTest
{
    @BeforeClass
    public static void beforeClass()
    {
        DatabaseDescriptor.daemonInitialization();
    }

    @Test
    public void spinningSlotsAreExclusiveAndReleased()
    {
        // no executor (and so no worker) is created by this test
        SharedExecutorPool pool = new SharedExecutorPool("SpinningSlotPool");
        Set<Integer> slots = new HashSet<>();
        for (int i = 0; i < SharedExecutorPool.MAX_SPINNING_WORKERS; i++)
        {
            int slot = pool.acquireSpinningSlot();
            assertThat(slot).isBetween(0, SharedExecutorPool.MAX_SPINNING_WORKERS - 1);
            assertThat(slots.add(slot)).isTrue();
        }
        assertThat(pool.acquireSpinningSlot()).isEqualTo(-1);

        for (int slot : slots)
            pool.releaseSpinningSlot(slot);

        assertThat(pool.spinningSlots.get()).isZero();
        for (int i = 0; i < SharedExecutorPool.MAX_SPINNING_WORKERS; i++)
            assertThat(pool.spinners.get(i)).isNull();
    }

    @Test
    public void spinningWorkerIsClaimedAndReleasesItsSlot() throws Throwable
    {
        SharedExecutorPool pool = new SharedExecutorPool("SpinningClaimPool");
        try
        {
            LocalAwareExecutorPlus executor = pool.newExecutor(1, "Stage", "SpinningClaim");
            CountDownLatch firstRan = new CountDownLatch(1);
            executor.execute(firstRan::countDown);
            assertThat(firstRan.await(1, TimeUnit.MINUTES)).isTrue();

            // the single worker has nothing left to do, so it registers itself as spinning
            SEPWorker spinner = null;
            long deadline = 0;
            for (int i = 0; i < 3000 && spinner == null; i++)
            {
                for (int slot = 0; slot < SharedExecutorPool.MAX_SPINNING_WORKERS; slot++)
                {
                    SEPWorker worker = pool.spinners.get(slot);
                    if (worker != null && (deadline = worker.spinDeadline()) > 0)
                    {
                        spinner = worker;
                        break;
                    }
                }
                if (spinner == null)
                    Thread.sleep(10);
            }
            assertThat(spinner).as("a worker registered itself as spinning").isNotNull();

            // the work must be handed to that worker (or, if it woke up first, to any other worker)
            CountDownLatch secondRan = new CountDownLatch(1);
            executor.execute(secondRan::countDown);
            assertThat(secondRan.await(1, TimeUnit.MINUTES)).isTrue();

            // a claimed spin is unregistered by the claimer, so the worker does not clear a later spin by mistake
            assertThat(spinner.claimSpin(deadline)).isFalse();
        }
        finally
        {
            pool.shutdownAndWait(1L, TimeUnit.MINUTES);
        }

        // every worker released its slot when it exited
        assertThat(pool.spinningSlots.get()).isZero();
        for (int i = 0; i < SharedExecutorPool.MAX_SPINNING_WORKERS; i++)
            assertThat(pool.spinners.get(i)).isNull();
    }

    @Test
    public void tasksAreServicedWhileWorkersSpin() throws Throwable
    {
        SharedExecutorPool pool = new SharedExecutorPool("SpinningTasksPool");
        try
        {
            int threads = 4;
            int tasksPerThread = 25000;
            LocalAwareExecutorPlus executor = pool.newExecutor(threads, "Stage", "SpinningTasks");
            CountDownLatch done = new CountDownLatch(threads * tasksPerThread);
            Thread[] submitters = new Thread[threads];
            for (int t = 0; t < threads; t++)
            {
                submitters[t] = new Thread(() -> {
                    for (int i = 0; i < tasksPerThread; i++)
                        executor.execute(done::countDown);
                });
                submitters[t].start();
            }
            for (Thread submitter : submitters)
                submitter.join(TimeUnit.MINUTES.toMillis(2));

            assertThat(done.await(2, TimeUnit.MINUTES)).isTrue();
        }
        finally
        {
            pool.shutdownAndWait(1L, TimeUnit.MINUTES);
        }
    }
}
