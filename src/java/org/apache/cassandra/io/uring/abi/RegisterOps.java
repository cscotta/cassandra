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
 * {@code IORING_REGISTER_*} opcodes (2nd argument of {@code io_uring_register}). {@link #PROBE} drives runtime
 * opcode-availability detection; the {@code *2} tagged variants are preferred for buffers/files where available.
 */
public final class RegisterOps
{
    public static final int REGISTER_BUFFERS = 0;
    public static final int UNREGISTER_BUFFERS = 1;
    public static final int REGISTER_FILES = 2;
    public static final int UNREGISTER_FILES = 3;
    public static final int REGISTER_EVENTFD = 4;
    public static final int UNREGISTER_EVENTFD = 5;
    public static final int REGISTER_FILES_UPDATE = 6;
    public static final int REGISTER_EVENTFD_ASYNC = 7;
    public static final int PROBE = 8;
    public static final int REGISTER_PERSONALITY = 9;
    public static final int UNREGISTER_PERSONALITY = 10;
    public static final int REGISTER_RESTRICTIONS = 11;
    public static final int ENABLE_RINGS = 12;
    public static final int REGISTER_FILES2 = 13;
    public static final int REGISTER_FILES_UPDATE2 = 14;
    public static final int REGISTER_BUFFERS2 = 15;
    public static final int REGISTER_BUFFERS_UPDATE = 16;
    public static final int REGISTER_RING_FDS = 20;
    public static final int UNREGISTER_RING_FDS = 21;
    public static final int REGISTER_PBUF_RING = 22;
    public static final int UNREGISTER_PBUF_RING = 23;
    public static final int REGISTER_SYNC_CANCEL = 24;
    public static final int REGISTER_FILE_ALLOC_RANGE = 25;

    /** OR into the register opcode to address the ring by its registered index. */
    public static final int USE_REGISTERED_RING = 1 << 31;

    /** {@code io_uring_probe_op.flags}: this opcode is supported by the running kernel. */
    public static final int IO_URING_OP_SUPPORTED = 1 << 0;

    /** {@code IORING_RSRC_REGISTER_SPARSE} flag for {@code REGISTER_*2}. */
    public static final int RSRC_REGISTER_SPARSE = 1 << 0;

    private RegisterOps() {}
}
