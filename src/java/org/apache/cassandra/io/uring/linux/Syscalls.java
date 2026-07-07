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
 * The three raw io_uring syscalls, bound as fixed-arity FFM downcall handles over libc's variadic {@code syscall(2)}.
 *
 * <p>io_uring has no glibc wrappers, so each syscall is a distinct {@link FunctionDescriptor} whose first argument
 * is the {@code long} syscall number (arg 0, non-variadic) followed by the concrete argument list, declared variadic
 * from index 1 via {@link Linker.Option#firstVariadicArg(int)} (required even on Linux for correct ABI setup).
 * Every handle captures {@code errno} into a leading {@link MemorySegment} so a {@code < 0} return can be decoded.
 *
 * <p>libc's {@code syscall()} wrapper follows the usual convention: it returns {@code -1} and sets {@code errno} on
 * error, so callers read errno from the capture segment only when the return is negative. Call sites use
 * {@code invokeExact} exclusively (never {@code invoke}) to avoid boxing and {@code asType} on the hot path.
 */
public final class Syscalls
{
    private static final Linker LINKER = Linker.nativeLinker();
    private static final MemorySegment SYSCALL = LINKER.defaultLookup().findOrThrow("syscall");

    // int io_uring_setup(u32 entries, struct io_uring_params *p)  ->  syscall(425, entries, params)
    private static final MethodHandle IO_URING_SETUP = LINKER.downcallHandle(
        SYSCALL,
        FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT, ADDRESS),
        Linker.Option.firstVariadicArg(1),
        Linker.Option.captureCallState("errno"));

    // int io_uring_enter(u32 fd, u32 to_submit, u32 min_complete, u32 flags, const void *arg, size_t argsz)
    private static final MethodHandle IO_URING_ENTER = LINKER.downcallHandle(
        SYSCALL,
        FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_LONG),
        Linker.Option.firstVariadicArg(1),
        Linker.Option.captureCallState("errno"));

    // int io_uring_register(u32 fd, u32 opcode, void *arg, u32 nr_args)
    private static final MethodHandle IO_URING_REGISTER = LINKER.downcallHandle(
        SYSCALL,
        FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT),
        Linker.Option.firstVariadicArg(1),
        Linker.Option.captureCallState("errno"));

    private Syscalls() {}

    /** Raw {@code io_uring_setup}; returns the ring fd ({@code >= 0}) or {@code -1} with {@code errno} in {@code cap}. */
    public static int setup(MemorySegment cap, int entries, MemorySegment params)
    {
        try
        {
            return (int) (long) IO_URING_SETUP.invokeExact(cap, Arch.SYS_IO_URING_SETUP, entries, params);
        }
        catch (Throwable t)
        {
            throw sneaky(t);
        }
    }

    /**
     * Raw {@code io_uring_enter}. Returns the number of SQEs consumed ({@code >= 0}) or {@code -1} with {@code errno}
     * in {@code cap}. {@code arg}/{@code argSz} are {@code MemorySegment.NULL}/{@code 0} for the plain form.
     */
    public static int enter(MemorySegment cap, int fd, int toSubmit, int minComplete, int flags,
                            MemorySegment arg, long argSz)
    {
        try
        {
            return (int) (long) IO_URING_ENTER.invokeExact(cap, Arch.SYS_IO_URING_ENTER,
                                                           fd, toSubmit, minComplete, flags, arg, argSz);
        }
        catch (Throwable t)
        {
            throw sneaky(t);
        }
    }

    /** Raw {@code io_uring_register}; returns {@code >= 0} or {@code -1} with {@code errno} in {@code cap}. */
    public static int register(MemorySegment cap, int fd, int opcode, MemorySegment arg, int nrArgs)
    {
        try
        {
            return (int) (long) IO_URING_REGISTER.invokeExact(cap, Arch.SYS_IO_URING_REGISTER,
                                                             fd, opcode, arg, nrArgs);
        }
        catch (Throwable t)
        {
            throw sneaky(t);
        }
    }

    private static RuntimeException sneaky(Throwable t)
    {
        if (t instanceof RuntimeException)
            return (RuntimeException) t;
        if (t instanceof Error)
            throw (Error) t;
        return new RuntimeException(t);
    }
}
