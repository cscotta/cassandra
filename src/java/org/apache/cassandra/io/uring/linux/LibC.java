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
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The handful of ordinary libc calls the ring lifecycle needs: {@code mmap}/{@code munmap} for the ring memory and
 * {@code close} for the ring fd. These are real glibc wrapper symbols (present in the default lookup), not raw
 * syscalls, so the wrapper handles {@code off_t} scaling and the {@code MAP_FAILED} return.
 */
public final class LibC
{
    // Linux mmap prot/flags.
    public static final int PROT_READ = 0x1;
    public static final int PROT_WRITE = 0x2;
    public static final int MAP_SHARED = 0x01;
    public static final int MAP_POPULATE = 0x8000;

    /** {@code getrlimit(2)} resource id for locked (pinned) memory; charged by registered io_uring buffers. */
    public static final int RLIMIT_MEMLOCK = 8;

    /** {@code eventfd2(2)} flags. */
    public static final int EFD_CLOEXEC = 0x80000;
    public static final int EFD_NONBLOCK = 0x800;

    /** mmap returns this on failure ((void *) -1). */
    public static final long MAP_FAILED = -1L;

    private static final Linker LINKER = Linker.nativeLinker();

    // void *mmap(void *addr, size_t len, int prot, int flags, int fd, off_t off)
    private static final MethodHandle MMAP = LINKER.downcallHandle(
        LINKER.defaultLookup().findOrThrow("mmap"),
        FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_LONG),
        Linker.Option.captureCallState("errno"));

    // int munmap(void *addr, size_t len) -- cleanup path, errno intentionally not captured.
    private static final MethodHandle MUNMAP = LINKER.downcallHandle(
        LINKER.defaultLookup().findOrThrow("munmap"),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG));

    // int close(int fd)
    private static final MethodHandle CLOSE = LINKER.downcallHandle(
        LINKER.defaultLookup().findOrThrow("close"),
        FunctionDescriptor.of(JAVA_INT, JAVA_INT),
        Linker.Option.captureCallState("errno"));

    // int getrlimit(int resource, struct rlimit *rlim)  -- real glibc wrapper, present since GLIBC_2.17
    private static final MethodHandle GETRLIMIT = LINKER.downcallHandle(
        LINKER.defaultLookup().findOrThrow("getrlimit"),
        FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS),
        Linker.Option.captureCallState("errno"));

    // int eventfd(unsigned int initval, int flags)  -- creates the poller-wakeup fd
    private static final MethodHandle EVENTFD = LINKER.downcallHandle(
        LINKER.defaultLookup().findOrThrow("eventfd"),
        FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT),
        Linker.Option.captureCallState("errno"));

    // int eventfd_write(int fd, uint64_t value)  -- fire-and-forget wakeup; errno intentionally not captured so it is
    // safe to call from any thread without a shared capture segment.
    private static final MethodHandle EVENTFD_WRITE = LINKER.downcallHandle(
        LINKER.defaultLookup().findOrThrow("eventfd_write"),
        FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_LONG));

    private LibC() {}

    /**
     * {@code mmap} a region of the ring fd. Returns a zero-length segment at the mapped address; the caller must
     * {@code reinterpret} it to the mapping length and register {@link #munmap} as the arena cleanup. On failure the
     * returned segment's {@link MemorySegment#address()} is {@link #MAP_FAILED} and {@code errno} is in {@code cap}.
     */
    public static MemorySegment mmap(MemorySegment cap, long len, int prot, int flags, int fd, long offset)
    {
        try
        {
            return (MemorySegment) MMAP.invokeExact(cap, MemorySegment.NULL, len, prot, flags, fd, offset);
        }
        catch (Throwable t)
        {
            throw rethrow(t);
        }
    }

    /** {@code munmap}; used as the arena cleanup action, so errno is ignored. */
    public static int munmap(MemorySegment addr, long len)
    {
        try
        {
            return (int) MUNMAP.invokeExact(addr, len);
        }
        catch (Throwable t)
        {
            throw rethrow(t);
        }
    }

    /** {@code close(fd)}; returns {@code 0} or {@code -1} with {@code errno} in {@code cap}. */
    public static int close(MemorySegment cap, int fd)
    {
        try
        {
            return (int) CLOSE.invokeExact(cap, fd);
        }
        catch (Throwable t)
        {
            throw rethrow(t);
        }
    }

    /**
     * {@code getrlimit(resource, rlim)} into a caller-owned {@code struct rlimit} segment (16 B: {@code rlim_cur},
     * {@code rlim_max}). Returns {@code 0} on success or {@code -1} with {@code errno} in {@code cap}. Used by the
     * buffer registrar to precheck {@link #RLIMIT_MEMLOCK} before pinning io_uring buffers.
     */
    public static int getrlimit(MemorySegment cap, int resource, MemorySegment rlimitOut)
    {
        try
        {
            return (int) GETRLIMIT.invokeExact(cap, resource, rlimitOut);
        }
        catch (Throwable t)
        {
            throw rethrow(t);
        }
    }

    /** {@code eventfd(initval, flags)}; returns the new fd or {@code -1} with {@code errno} in {@code cap}. */
    public static int eventfd(MemorySegment cap, int initval, int flags)
    {
        try
        {
            return (int) EVENTFD.invokeExact(cap, initval, flags);
        }
        catch (Throwable t)
        {
            throw rethrow(t);
        }
    }

    /**
     * {@code eventfd_write(fd, value)} &mdash; adds {@code value} to the eventfd counter, waking anything blocked
     * reading it. Fire-and-forget (errno not captured), so it is safe to call from any thread; returns the raw result.
     */
    public static int eventfdWrite(int fd, long value)
    {
        try
        {
            return (int) EVENTFD_WRITE.invokeExact(fd, value);
        }
        catch (Throwable t)
        {
            throw rethrow(t);
        }
    }

    private static RuntimeException rethrow(Throwable t)
    {
        if (t instanceof RuntimeException)
            return (RuntimeException) t;
        if (t instanceof Error)
            throw (Error) t;
        return new RuntimeException(t);
    }
}
