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
 * {@code IOSQE_*} per-SQE flags ({@code sqe.flags}). For independent chunk reads Cassandra submits unlinked, with no
 * drain, one {@code user_data} per read, reaping out of order; {@link #FIXED_FILE} and {@link #IO_LINK} are the only
 * storage-relevant flags (registered files, and ordered readahead if ever needed).
 */
public final class SqeFlags
{
    public static final int FIXED_FILE = 1 << 0;
    public static final int IO_DRAIN = 1 << 1;
    public static final int IO_LINK = 1 << 2;
    public static final int IO_HARDLINK = 1 << 3;
    public static final int ASYNC = 1 << 4;
    public static final int BUFFER_SELECT = 1 << 5;
    public static final int CQE_SKIP_SUCCESS = 1 << 6;

    private SqeFlags() {}
}
