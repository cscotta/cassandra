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

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;
import static java.lang.foreign.ValueLayout.JAVA_LONG_UNALIGNED;

/**
 * Thin, allocation-free accessors over a 16-byte {@code io_uring_cqe} slice of the CQ mmap.
 *
 * <p>{@code res} carries the transferred byte count when {@code >= 0}; a value {@code < 0} is {@code -errno};
 * {@code res == 0} on a read is EOF; {@code 0 < res < len} is a short read whose remainder must be resubmitted.
 */
public final class Cqe
{
    private Cqe() {}

    public static long userData(MemorySegment cqe)
    {
        return cqe.get(JAVA_LONG_UNALIGNED, Layouts.CQE_USER_DATA);
    }

    public static int res(MemorySegment cqe)
    {
        return cqe.get(JAVA_INT_UNALIGNED, Layouts.CQE_RES);
    }

    public static int flags(MemorySegment cqe)
    {
        return cqe.get(JAVA_INT_UNALIGNED, Layouts.CQE_FLAGS);
    }
}
