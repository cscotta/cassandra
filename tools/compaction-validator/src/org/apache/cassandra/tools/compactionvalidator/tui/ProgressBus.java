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
package org.apache.cassandra.tools.compactionvalidator.tui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Thread-safe event queue used to decouple the producer threads (data
 * generator, compaction drivers, validator) from the single render thread that
 * drains and dispatches events to {@link TuiPanel}s.
 *
 * <p>{@link #push} is non-blocking; {@link #drain} returns and removes the
 * full pending batch so the render thread can apply state updates atomically.
 */
public class ProgressBus
{
    private final BlockingQueue<TuiEvent> queue = new LinkedBlockingQueue<>();

    /**
     * Enqueues {@code event}.  Never blocks.
     *
     * @param event event to enqueue (must not be null)
     */
    public void push(TuiEvent event)
    {
        if (event == null)
            throw new IllegalArgumentException("event must not be null");
        queue.offer(event);
    }

    /**
     * Returns and removes the next event from the queue, or {@code null} if
     * the queue is empty.
     *
     * @return the next event, or {@code null}
     */
    public TuiEvent poll()
    {
        return queue.poll();
    }

    /**
     * Returns and removes <em>all</em> currently pending events as a list,
     * preserving insertion order.  The returned list is a fresh copy that the
     * caller may mutate freely.
     *
     * @return the pending events (possibly empty, never {@code null})
     */
    public List<TuiEvent> drain()
    {
        List<TuiEvent> events = new ArrayList<>(queue.size());
        queue.drainTo(events);
        return events;
    }

    /** Returns {@code true} if no events are currently pending. */
    public boolean isEmpty()
    {
        return queue.isEmpty();
    }

    /** Returns the number of currently pending events. */
    public int size()
    {
        return queue.size();
    }
}
