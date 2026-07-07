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
 * {@code IORING_FEAT_*} flags returned in {@code io_uring_params.features} after setup. The binding records these
 * once and gates optional paths on them &mdash; e.g. {@link #SINGLE_MMAP} decides 2-vs-3 mmaps, {@link #NODROP}
 * guarantees the kernel backlogs CQ overflow rather than dropping completions, {@link #EXT_ARG} enables a
 * wait-with-timeout without a separate timeout SQE.
 */
public final class FeatureFlags
{
    public static final int SINGLE_MMAP = 1 << 0;
    public static final int NODROP = 1 << 1;
    public static final int SUBMIT_STABLE = 1 << 2;
    public static final int RW_CUR_POS = 1 << 3;
    public static final int CUR_PERSONALITY = 1 << 4;
    public static final int FAST_POLL = 1 << 5;
    public static final int POLL_32BITS = 1 << 6;
    public static final int SQPOLL_NONFIXED = 1 << 7;
    public static final int EXT_ARG = 1 << 8;
    public static final int NATIVE_WORKERS = 1 << 9;
    public static final int RSRC_TAGS = 1 << 10;
    public static final int CQE_SKIP = 1 << 11;
    public static final int LINKED_FILE = 1 << 12;
    public static final int REG_REG_RING = 1 << 13;

    private FeatureFlags() {}

    public static boolean has(int features, int flag)
    {
        return (features & flag) != 0;
    }
}
