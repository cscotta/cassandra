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
package org.apache.cassandra.io.uring.abi;

/**
 * {@code IORING_SQ_*} flags read from the SQ ring's {@code flags} word (kernel-owned). Under SQPOLL,
 * {@link #NEED_WAKEUP} tells the submitter to wake a parked poll thread; {@link #CQ_OVERFLOW} tells any reaper to
 * flush the kernel's overflow backlog with a {@code GETEVENTS} enter; {@link #TASKRUN} signals deferred task-work.
 */
public final class SqCqFlags
{
    public static final int NEED_WAKEUP = 1 << 0;
    public static final int CQ_OVERFLOW = 1 << 1;
    public static final int TASKRUN = 1 << 2;

    private SqCqFlags() {}
}
