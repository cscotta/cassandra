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
package org.apache.cassandra.io.uring;

/**
 * The library's typed error. Carries the raw {@code errno} (or negated CQE {@code res}) when the failure
 * originated from a syscall or a completion, or {@link #NO_ERRNO} when it did not.
 *
 * <p>This type lives in the extractable core (no Cassandra dependency). The Cassandra adapter is responsible
 * for translating it into the storage engine's {@code FSReadError}/{@code CorruptSSTableException} where
 * appropriate.
 */
public class IoUringException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /** Sentinel used when the exception does not carry a meaningful errno. */
    public static final int NO_ERRNO = 0;

    private final int errno;

    public IoUringException(String message)
    {
        this(message, NO_ERRNO);
    }

    public IoUringException(String message, int errno)
    {
        super(errno == NO_ERRNO ? message : message + " (errno=" + errno + ')');
        this.errno = errno;
    }

    public IoUringException(String message, Throwable cause)
    {
        super(message, cause);
        this.errno = NO_ERRNO;
    }

    /** The errno associated with this failure, or {@link #NO_ERRNO} if none. */
    public int errno()
    {
        return errno;
    }
}
