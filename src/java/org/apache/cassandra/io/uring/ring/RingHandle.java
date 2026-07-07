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
package org.apache.cassandra.io.uring.ring;

import java.lang.foreign.MemorySegment;

/**
 * The narrow ring surface that {@link RingSubmitter} (and, above it, the async event loop) drive: the SQ/CQ views, the
 * ring fd, feature params, the reused errno-capture segment, the kernel-owned SQ flags word, and the one true kernel
 * boundary {@link #enter}. The production implementation is {@link Ring}, backed by a live io_uring instance whose
 * {@link #enter} forwards to the {@code io_uring_enter} syscall.
 *
 * <p>Lifting only {@link #enter} to an interface method lets a test fake implement this over ordinary (non-mmap)
 * memory and make {@link #enter} an in-process kernel that consumes the submitted SQEs and writes synthetic CQEs into
 * the real queue segments &mdash; so the real {@link SubmissionQueue}/{@link CompletionQueue} cursors and barriers and
 * the full {@link RingSubmitter} submit/reap logic run unchanged, and a fault is expressed purely as the {@code
 * res}/{@code flags}/timing/ordering of the completions the fake produces.
 */
public interface RingHandle extends AutoCloseable
{
    int fd();

    RingParams params();

    SubmissionQueue sq();

    CompletionQueue cq();

    /** The reused errno-capture segment; after {@link #enter} returns a negative value the errno is read from here. */
    MemorySegment captureSegment();

    /** Current SQ flags word (kernel-owned): carries {@code IORING_SQ_CQ_OVERFLOW}/{@code TASKRUN}. */
    int sqFlags();

    /**
     * The {@code io_uring_enter} boundary: submit {@code toSubmit} prepared SQEs, wait for {@code waitNr} completions,
     * with {@code flags}. Returns the number of SQEs consumed ({@code >= 0}) on success, or a negative value with the
     * errno recorded in {@link #captureSegment()} &mdash; mirroring the raw syscall so {@link RingSubmitter}'s decode
     * loop is byte-for-byte unchanged.
     */
    int enter(int toSubmit, int waitNr, int flags);

    @Override
    void close();
}
