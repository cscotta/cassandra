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
package org.apache.cassandra.io.uring.op;

import java.lang.foreign.MemorySegment;

import org.apache.cassandra.io.uring.abi.OpFlags;
import org.apache.cassandra.io.uring.abi.SqeExt;

/**
 * Encodes the {@code sqe.file_index} field for {@code *_direct} ops (mirrors liburing's
 * {@code __io_uring_set_target_fixed_file}). The kernel reserves {@code 0} to mean "not a fixed-file op", so a real
 * registered-file slot index is stored as {@code index + 1}; the special {@link #ALLOC} sentinel is stored verbatim
 * to ask io_uring to pick any free slot (the chosen index comes back in {@code cqe.res}).
 */
public final class TargetFixedFile
{
    /** Ask io_uring to allocate a free registered-file slot rather than targeting a specific one. */
    public static final int ALLOC = OpFlags.IORING_FILE_INDEX_ALLOC;

    private TargetFixedFile() {}

    /**
     * Writes {@code sqe.file_index}: {@link #ALLOC} verbatim, otherwise {@code fileIndexOrAlloc + 1} so that a stored
     * zero can keep meaning "no fixed file". Call after the base prep, on any op that installs a direct descriptor
     * ({@code openat_direct}, {@code accept_direct}, {@code socket_direct}, {@code close_direct}, ...).
     */
    public static void set(MemorySegment sqe, int fileIndexOrAlloc)
    {
        SqeExt.fileIndex(sqe, fileIndexOrAlloc == ALLOC ? ALLOC : fileIndexOrAlloc + 1);
    }
}
