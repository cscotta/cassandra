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
package org.apache.cassandra.io.uring.linux;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Linux {@code errno} constants (asm-generic values, identical on x86-64 and aarch64), the FFM
 * {@code captureCallState("errno")} layout used to read errno after a failed control syscall, and a
 * {@code strerror} helper for diagnostics.
 *
 * <p>Per-I/O results never use errno &mdash; the CQE {@code res} field carries {@code -errno} directly &mdash;
 * so errno capture is entirely off the data hot path (control syscalls plus {@code mmap} only).
 */
public final class Errno
{
    // asm-generic errno.h values (same on x86-64 and aarch64).
    public static final int EPERM = 1;
    public static final int EINTR = 4;
    public static final int EIO = 5;
    public static final int EBADF = 9;
    public static final int EAGAIN = 11;
    public static final int ENOMEM = 12;
    public static final int EACCES = 13;
    public static final int EFAULT = 14;
    public static final int EBUSY = 16;
    public static final int EINVAL = 22;
    public static final int EMFILE = 24;
    public static final int ENOSYS = 38;
    public static final int EOPNOTSUPP = 95;
    public static final int ETIME = 62;
    public static final int ECANCELED = 125;

    /** The platform capture-state layout; on Linux it contains a member named {@code "errno"}. */
    public static final StructLayout CAPTURE = Linker.Option.captureStateLayout();

    private static final VarHandle ERRNO_VH =
        CAPTURE.varHandle(MemoryLayout.PathElement.groupElement("errno"));

    private static final MethodHandle STRERROR = Linker.nativeLinker().downcallHandle(
        Linker.nativeLinker().defaultLookup().findOrThrow("strerror"),
        FunctionDescriptor.of(ADDRESS, JAVA_INT));

    private Errno() {}

    /** Reads the captured {@code errno} out of a segment allocated with {@link #CAPTURE}. */
    public static int of(MemorySegment captureSegment)
    {
        return (int) ERRNO_VH.get(captureSegment, 0L);
    }

    /** Best-effort {@code strerror(3)} for diagnostics; never throws. */
    public static String strerror(int errno)
    {
        try
        {
            MemorySegment p = (MemorySegment) STRERROR.invokeExact(errno);
            if (p.address() == 0L)
                return "errno " + errno;
            return p.reinterpret(256).getString(0);
        }
        catch (Throwable t)
        {
            return "errno " + errno;
        }
    }
}
