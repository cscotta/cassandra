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
 * {@code IORING_OP_*} opcodes (the {@code enum io_uring_op} ordinal is the opcode number).
 *
 * <p>The full 0..63 surface is listed for library completeness, but only opcodes at or below the kernel floor
 * (5.6, {@code READ}=22) are safe to assume present; anything numerically higher must be gated at runtime via
 * {@code IORING_REGISTER_PROBE} rather than trusted because a constant exists here. Cassandra's read path uses
 * {@link #READ}, {@link #READV}, {@link #READ_FIXED}, {@link #READV_FIXED}, {@link #FSYNC} and {@link #NOP}.
 */
public final class Opcodes
{
    public static final int NOP = 0;
    public static final int READV = 1;
    public static final int WRITEV = 2;
    public static final int FSYNC = 3;
    public static final int READ_FIXED = 4;
    public static final int WRITE_FIXED = 5;
    public static final int POLL_ADD = 6;
    public static final int POLL_REMOVE = 7;
    public static final int SYNC_FILE_RANGE = 8;
    public static final int SENDMSG = 9;
    public static final int RECVMSG = 10;
    public static final int TIMEOUT = 11;
    public static final int TIMEOUT_REMOVE = 12;
    public static final int ACCEPT = 13;
    public static final int ASYNC_CANCEL = 14;
    public static final int LINK_TIMEOUT = 15;
    public static final int CONNECT = 16;
    public static final int FALLOCATE = 17;
    public static final int OPENAT = 18;
    public static final int CLOSE = 19;
    public static final int FILES_UPDATE = 20;
    public static final int STATX = 21;
    public static final int READ = 22;
    public static final int WRITE = 23;
    public static final int FADVISE = 24;
    public static final int MADVISE = 25;
    public static final int SEND = 26;
    public static final int RECV = 27;
    public static final int OPENAT2 = 28;
    public static final int EPOLL_CTL = 29;
    public static final int SPLICE = 30;
    public static final int PROVIDE_BUFFERS = 31;
    public static final int REMOVE_BUFFERS = 32;
    public static final int TEE = 33;
    public static final int SHUTDOWN = 34;
    public static final int RENAMEAT = 35;
    public static final int UNLINKAT = 36;
    public static final int MKDIRAT = 37;
    public static final int SYMLINKAT = 38;
    public static final int LINKAT = 39;
    public static final int MSG_RING = 40;
    public static final int FSETXATTR = 41;
    public static final int SETXATTR = 42;
    public static final int FGETXATTR = 43;
    public static final int GETXATTR = 44;
    public static final int SOCKET = 45;
    public static final int URING_CMD = 46;
    public static final int SEND_ZC = 47;
    public static final int SENDMSG_ZC = 48;
    public static final int READ_MULTISHOT = 49;
    public static final int WAITID = 50;
    public static final int FUTEX_WAIT = 51;
    public static final int FUTEX_WAKE = 52;
    public static final int FUTEX_WAITV = 53;
    public static final int FIXED_FD_INSTALL = 54;
    public static final int FTRUNCATE = 55;
    public static final int BIND = 56;
    public static final int LISTEN = 57;
    public static final int RECV_ZC = 58;
    public static final int EPOLL_WAIT = 59;
    public static final int READV_FIXED = 60;
    public static final int WRITEV_FIXED = 61;

    /** Highest opcode this table names; used to bound a {@code REGISTER_PROBE} scan. */
    public static final int LAST_KNOWN = WRITEV_FIXED;

    /** {@code fsync_flags}: fdatasync rather than a full fsync. */
    public static final int IORING_FSYNC_DATASYNC = 1 << 0;

    private Opcodes() {}
}
