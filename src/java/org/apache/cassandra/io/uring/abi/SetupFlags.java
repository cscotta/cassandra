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
 * {@code IORING_SETUP_*} flags for {@code io_uring_params.flags}. The baseline read path uses {@code 0}; the
 * latency opt-ins ({@link #SINGLE_ISSUER}/{@link #DEFER_TASKRUN}) require a feature/version probe (absent on RHEL 9
 * / Ubuntu 22.04) and are enabled only for the one-poller-per-ring model on kernels &ge; 6.1.
 */
public final class SetupFlags
{
    public static final int IOPOLL = 1 << 0;
    public static final int SQPOLL = 1 << 1;
    public static final int SQ_AFF = 1 << 2;
    public static final int CQSIZE = 1 << 3;
    public static final int CLAMP = 1 << 4;
    public static final int ATTACH_WQ = 1 << 5;
    public static final int R_DISABLED = 1 << 6;
    public static final int SUBMIT_ALL = 1 << 7;
    public static final int COOP_TASKRUN = 1 << 8;
    public static final int TASKRUN_FLAG = 1 << 9;
    public static final int SQE128 = 1 << 10;
    public static final int CQE32 = 1 << 11;
    public static final int SINGLE_ISSUER = 1 << 12;
    public static final int DEFER_TASKRUN = 1 << 13;
    public static final int NO_MMAP = 1 << 14;
    public static final int REGISTERED_FD_ONLY = 1 << 15;
    public static final int NO_SQARRAY = 1 << 16;

    private SetupFlags() {}
}
