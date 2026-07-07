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
 * {@code IORING_CQE_F_*} completion flags ({@code cqe.flags}). {@link #MORE} (multishot: more CQEs coming, do not
 * retire the in-flight entry yet) and the provided-buffer id decode ({@code flags >> }{@link #BUFFER_SHIFT}) are the
 * subtle ones; the storage read path uses none of these but the dispatcher must respect {@link #MORE}.
 */
public final class CqeFlags
{
    public static final int BUFFER = 1 << 0;
    public static final int MORE = 1 << 1;
    public static final int SOCK_NONEMPTY = 1 << 2;
    public static final int NOTIF = 1 << 3;
    public static final int BUF_MORE = 1 << 4;

    /** Buffer id occupies the upper 16 bits of {@code cqe.flags} when {@link #BUFFER} is set. */
    public static final int BUFFER_SHIFT = 16;

    private CqeFlags() {}

    public static boolean hasMore(int cqeFlags)
    {
        return (cqeFlags & MORE) != 0;
    }
}
