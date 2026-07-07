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

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;

import org.apache.cassandra.io.uring.IoUringException;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM {@link StructLayout}s for every io_uring ABI struct, plus the ground-truth field offsets used for
 * offset-based hot-path access. All sizes and offsets were compiler-verified against
 * {@code /usr/include/linux/io_uring.h}; {@link #verify()} re-checks the constructed layouts against those constants
 * at startup so a wrong {@code paddingLayout} (FFM does <em>not</em> auto-pad) or a bad offset fails loudly rather
 * than corrupting the ring.
 *
 * <p>Hot-path SQE/CQE access uses the plain {@code *_OFF} integer offsets (see {@link Sqe}/{@link Cqe}); the
 * {@link StructLayout} objects exist for the layout guard and for callers that prefer a path-based {@code VarHandle}.
 */
public final class Layouts
{
    // --- struct io_uring_params (120 B) ---
    public static final int PARAMS_BYTES = 120;
    public static final int PARAMS_SQ_ENTRIES_OFF = 0;
    public static final int PARAMS_CQ_ENTRIES_OFF = 4;
    public static final int PARAMS_FLAGS_OFF = 8;
    public static final int PARAMS_FEATURES_OFF = 20;
    public static final int PARAMS_SQ_OFF = 40;
    public static final int PARAMS_CQ_OFF = 80;

    // --- struct io_sqring_offsets (40 B), fields relative to the struct base ---
    public static final int SQOFF_BYTES = 40;
    public static final int SQOFF_HEAD = 0;
    public static final int SQOFF_TAIL = 4;
    public static final int SQOFF_RING_MASK = 8;
    public static final int SQOFF_RING_ENTRIES = 12;
    public static final int SQOFF_FLAGS = 16;
    public static final int SQOFF_DROPPED = 20;
    public static final int SQOFF_ARRAY = 24;

    // --- struct io_cqring_offsets (40 B) ---
    public static final int CQOFF_BYTES = 40;
    public static final int CQOFF_HEAD = 0;
    public static final int CQOFF_TAIL = 4;
    public static final int CQOFF_RING_MASK = 8;
    public static final int CQOFF_RING_ENTRIES = 12;
    public static final int CQOFF_OVERFLOW = 16;
    public static final int CQOFF_CQES = 20;
    public static final int CQOFF_FLAGS = 24;

    // --- struct io_uring_sqe (64 B) ---
    public static final int SQE_BYTES = 64;
    public static final int SQE_OPCODE = 0;
    public static final int SQE_FLAGS = 1;
    public static final int SQE_IOPRIO = 2;
    public static final int SQE_FD = 4;
    public static final int SQE_OFF = 8;
    public static final int SQE_ADDR = 16;
    public static final int SQE_LEN = 24;
    public static final int SQE_RW_FLAGS = 28;
    public static final int SQE_USER_DATA = 32;
    public static final int SQE_BUF_INDEX = 40;

    // --- struct io_uring_cqe (16 B) ---
    public static final int CQE_BYTES = 16;
    public static final int CQE_USER_DATA = 0;
    public static final int CQE_RES = 8;
    public static final int CQE_FLAGS = 12;

    // --- struct iovec / __kernel_timespec (16 B each) ---
    public static final int IOVEC_BYTES = 16;
    public static final int IOVEC_BASE = 0;
    public static final int IOVEC_LEN = 8;
    public static final int TIMESPEC_BYTES = 16;
    public static final int TIMESPEC_SEC = 0;
    public static final int TIMESPEC_NSEC = 8;

    public static final StructLayout SQ_OFFSETS = MemoryLayout.structLayout(
        JAVA_INT.withName("head"),
        JAVA_INT.withName("tail"),
        JAVA_INT.withName("ring_mask"),
        JAVA_INT.withName("ring_entries"),
        JAVA_INT.withName("flags"),
        JAVA_INT.withName("dropped"),
        JAVA_INT.withName("array"),
        JAVA_INT.withName("resv1"),
        JAVA_LONG.withName("user_addr")).withName("io_sqring_offsets");

    public static final StructLayout CQ_OFFSETS = MemoryLayout.structLayout(
        JAVA_INT.withName("head"),
        JAVA_INT.withName("tail"),
        JAVA_INT.withName("ring_mask"),
        JAVA_INT.withName("ring_entries"),
        JAVA_INT.withName("overflow"),
        JAVA_INT.withName("cqes"),
        JAVA_INT.withName("flags"),
        JAVA_INT.withName("resv1"),
        JAVA_LONG.withName("user_addr")).withName("io_cqring_offsets");

    public static final StructLayout PARAMS = MemoryLayout.structLayout(
        JAVA_INT.withName("sq_entries"),
        JAVA_INT.withName("cq_entries"),
        JAVA_INT.withName("flags"),
        JAVA_INT.withName("sq_thread_cpu"),
        JAVA_INT.withName("sq_thread_idle"),
        JAVA_INT.withName("features"),
        JAVA_INT.withName("wq_fd"),
        MemoryLayout.sequenceLayout(3, JAVA_INT).withName("resv"),
        SQ_OFFSETS.withName("sq_off"),
        CQ_OFFSETS.withName("cq_off")).withName("io_uring_params");

    public static final StructLayout SQE = MemoryLayout.structLayout(
        java.lang.foreign.ValueLayout.JAVA_BYTE.withName("opcode"),
        java.lang.foreign.ValueLayout.JAVA_BYTE.withName("flags"),
        java.lang.foreign.ValueLayout.JAVA_SHORT.withName("ioprio"),
        JAVA_INT.withName("fd"),
        JAVA_LONG.withName("off"),
        JAVA_LONG.withName("addr"),
        JAVA_INT.withName("len"),
        JAVA_INT.withName("rw_flags"),
        JAVA_LONG.withName("user_data"),
        java.lang.foreign.ValueLayout.JAVA_SHORT.withName("buf_index"),
        java.lang.foreign.ValueLayout.JAVA_SHORT.withName("personality"),
        JAVA_INT.withName("splice_fd_in"),
        JAVA_LONG.withName("addr3"),
        JAVA_LONG.withName("pad2")).withName("io_uring_sqe");

    public static final StructLayout CQE = MemoryLayout.structLayout(
        JAVA_LONG.withName("user_data"),
        JAVA_INT.withName("res"),
        JAVA_INT.withName("flags")).withName("io_uring_cqe");

    public static final StructLayout IOVEC = MemoryLayout.structLayout(
        ADDRESS.withName("iov_base"),
        JAVA_LONG.withName("iov_len")).withName("iovec");

    public static final StructLayout KERNEL_TIMESPEC = MemoryLayout.structLayout(
        JAVA_LONG.withName("tv_sec"),
        JAVA_LONG.withName("tv_nsec")).withName("__kernel_timespec");

    private Layouts() {}

    /**
     * Asserts every constructed layout matches the compiler-verified ground truth. Cheap; call once at startup and
     * in the ABI-layout test. Throws {@link IoUringException} on any mismatch (a padding or offset bug).
     */
    public static void verify()
    {
        check("params.byteSize", PARAMS.byteSize(), PARAMS_BYTES);
        check("params.features", PARAMS.byteOffset(groupElement("features")), PARAMS_FEATURES_OFF);
        check("params.sq_off", PARAMS.byteOffset(groupElement("sq_off")), PARAMS_SQ_OFF);
        check("params.cq_off", PARAMS.byteOffset(groupElement("cq_off")), PARAMS_CQ_OFF);

        check("sq_off.byteSize", SQ_OFFSETS.byteSize(), SQOFF_BYTES);
        check("sq_off.array", SQ_OFFSETS.byteOffset(groupElement("array")), SQOFF_ARRAY);
        check("sq_off.tail", SQ_OFFSETS.byteOffset(groupElement("tail")), SQOFF_TAIL);

        check("cq_off.byteSize", CQ_OFFSETS.byteSize(), CQOFF_BYTES);
        check("cq_off.cqes", CQ_OFFSETS.byteOffset(groupElement("cqes")), CQOFF_CQES);
        check("cq_off.tail", CQ_OFFSETS.byteOffset(groupElement("tail")), CQOFF_TAIL);

        check("sqe.byteSize", SQE.byteSize(), SQE_BYTES);
        check("sqe.addr", SQE.byteOffset(groupElement("addr")), SQE_ADDR);
        check("sqe.len", SQE.byteOffset(groupElement("len")), SQE_LEN);
        check("sqe.user_data", SQE.byteOffset(groupElement("user_data")), SQE_USER_DATA);
        check("sqe.buf_index", SQE.byteOffset(groupElement("buf_index")), SQE_BUF_INDEX);

        check("cqe.byteSize", CQE.byteSize(), CQE_BYTES);
        check("cqe.res", CQE.byteOffset(groupElement("res")), CQE_RES);
        check("cqe.flags", CQE.byteOffset(groupElement("flags")), CQE_FLAGS);

        check("iovec.byteSize", IOVEC.byteSize(), IOVEC_BYTES);
        check("timespec.byteSize", KERNEL_TIMESPEC.byteSize(), TIMESPEC_BYTES);
    }

    private static void check(String what, long actual, long expected)
    {
        if (actual != expected)
            throw new IoUringException("ABI layout mismatch for " + what + ": expected " + expected + " but got " + actual);
    }
}
