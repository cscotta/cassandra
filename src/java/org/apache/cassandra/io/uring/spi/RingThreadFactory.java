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
package org.apache.cassandra.io.uring.spi;

/**
 * SPI seam for creating the thread(s) that run a ring's poll/reap loop. The extractable core never spawns threads
 * itself; it asks this factory for one, so the Cassandra adapter can route ring threads through its own
 * {@code ExecutorFactory}/naming conventions and thread hierarchy. The returned thread must be unstarted; the caller
 * starts it once it is fully wired up.
 */
public interface RingThreadFactory
{
    /**
     * Create (but do not start) a thread that will run {@code loop} under the given {@code name}. Implementations
     * decide daemon status, priority, and grouping; the loop itself is supplied by the ring.
     */
    Thread newRingThread(String name, Runnable loop);

    /**
     * Default implementation: a plain daemon thread carrying the requested name, so a lingering ring thread never
     * keeps the JVM alive on shutdown.
     */
    final class DaemonRingThreadFactory implements RingThreadFactory
    {
        /** Shared stateless instance. */
        public static final DaemonRingThreadFactory INSTANCE = new DaemonRingThreadFactory();

        public DaemonRingThreadFactory()
        {
        }

        @Override
        public Thread newRingThread(String name, Runnable loop)
        {
            Thread thread = new Thread(loop, name); // checkstyle: permit this instantiation
            thread.setDaemon(true);
            return thread;
        }
    }
}
