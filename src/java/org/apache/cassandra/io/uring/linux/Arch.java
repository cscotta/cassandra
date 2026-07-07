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
package org.apache.cassandra.io.uring.linux;

/**
 * Per-architecture io_uring syscall numbers.
 *
 * <p>The three io_uring syscalls use the {@code asm-generic} numbering, which is <em>identical</em> on the two
 * architectures Cassandra targets (x86-64 and aarch64): {@code setup=425}, {@code enter=426}, {@code register=427}.
 * They are therefore constants here. This class exists to localize the one place a future architecture with a
 * divergent table would be handled &mdash; the resolution would be fed in through an injected property provider
 * rather than a direct {@code System.getProperty} (which the tree's checkstyle bans), keeping the core free of
 * ambient global state.
 */
public final class Arch
{
    public static final long SYS_IO_URING_SETUP = 425L;
    public static final long SYS_IO_URING_ENTER = 426L;
    public static final long SYS_IO_URING_REGISTER = 427L;

    private Arch() {}
}
