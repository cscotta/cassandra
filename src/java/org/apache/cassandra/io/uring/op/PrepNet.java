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
package org.apache.cassandra.io.uring.op;

import java.lang.foreign.MemorySegment;

import org.apache.cassandra.io.uring.abi.OpFlags;
import org.apache.cassandra.io.uring.abi.Opcodes;
import org.apache.cassandra.io.uring.abi.Sqe;
import org.apache.cassandra.io.uring.abi.SqeExt;

/**
 * Socket-op preps mirroring liburing's {@code io_uring_prep_*} network wrappers. Present for library completeness;
 * Cassandra's storage engine does not submit socket ops through this ring (the native transport is Netty's job).
 *
 * <p>Every prep first calls {@link Prep#prepRw} (which zeroes the whole 64-byte SQE, so no stale union byte can leak)
 * and then sets only the op-specific extras via {@link Sqe}/{@link SqeExt}. Buffer/{@code sockaddr}/{@code msghdr}
 * arguments are raw native addresses (the caller owns the backing {@code MemorySegment} and must keep it alive until
 * the CQE is reaped). Direct variants install a registered-file descriptor via {@link TargetFixedFile}.
 */
public final class PrepNet
{
    private PrepNet() {}

    /**
     * {@code IORING_OP_ACCEPT}: accept4 on {@code fd}. {@code sockaddrAddr}/{@code addrLenAddr} are native pointers to
     * the out {@code sockaddr} and its {@code socklen_t} (either may be 0 to ignore the peer address);
     * {@code acceptFlags} are {@code SOCK_*} accept4 flags.
     */
    public static void accept(MemorySegment sqe, int fd, long sockaddrAddr, long addrLenAddr, int acceptFlags)
    {
        Prep.prepRw(sqe, Opcodes.ACCEPT, fd, sockaddrAddr, 0, addrLenAddr);
        SqeExt.opFlags(sqe, acceptFlags);
    }

    /**
     * {@code IORING_OP_ACCEPT} into a registered-file slot: like {@link #accept} but the new connection is installed
     * as a fixed descriptor at {@code fileIndex} (or {@link TargetFixedFile#ALLOC} to auto-allocate).
     */
    public static void acceptDirect(MemorySegment sqe, int fd, long sockaddrAddr, long addrLenAddr,
                                    int acceptFlags, int fileIndex)
    {
        accept(sqe, fd, sockaddrAddr, addrLenAddr, acceptFlags);
        TargetFixedFile.set(sqe, fileIndex);
    }

    /**
     * {@code IORING_OP_ACCEPT} multishot: stays armed, emitting one CQE per accepted connection (each flagged
     * {@code CQE_F_MORE} until the last). Sets {@link OpFlags#IORING_ACCEPT_MULTISHOT} in {@code ioprio}.
     */
    public static void multishotAccept(MemorySegment sqe, int fd, long sockaddrAddr, long addrLenAddr, int acceptFlags)
    {
        accept(sqe, fd, sockaddrAddr, addrLenAddr, acceptFlags);
        Sqe.ioprio(sqe, OpFlags.IORING_ACCEPT_MULTISHOT);
    }

    /**
     * {@code IORING_OP_CONNECT}: connect {@code fd} to the {@code sockaddr} at {@code sockaddrAddr} of length
     * {@code addrLen} (the length rides in the {@code off} field).
     */
    public static void connect(MemorySegment sqe, int fd, long sockaddrAddr, int addrLen)
    {
        Prep.prepRw(sqe, Opcodes.CONNECT, fd, sockaddrAddr, 0, addrLen);
    }

    /** {@code IORING_OP_SEND}: send {@code len} bytes from {@code bufAddr} on {@code fd}; {@code msgFlags} are send(2) flags. */
    public static void send(MemorySegment sqe, int fd, long bufAddr, int len, int msgFlags)
    {
        Prep.prepRw(sqe, Opcodes.SEND, fd, bufAddr, len, 0L);
        SqeExt.opFlags(sqe, msgFlags);
    }

    /**
     * {@code IORING_OP_SEND_ZC}: zero-copy send; emits a first CQE (result, possibly {@code CQE_F_MORE}) then a second
     * {@code CQE_F_NOTIF} once the buffer is safe to reuse. {@code zcFlags} (e.g. {@link OpFlags#IORING_SEND_ZC_REPORT_USAGE})
     * ride in {@code ioprio}.
     */
    public static void sendZc(MemorySegment sqe, int fd, long bufAddr, int len, int msgFlags, int zcFlags)
    {
        Prep.prepRw(sqe, Opcodes.SEND_ZC, fd, bufAddr, len, 0L);
        SqeExt.opFlags(sqe, msgFlags);
        Sqe.ioprio(sqe, zcFlags);
    }

    /**
     * {@code IORING_OP_SEND} to an unconnected socket: like {@link #send} plus the destination address
     * ({@code destAddr}/{@code destAddrLen}) written through the {@code addr2}/{@code addr_len} union.
     */
    public static void sendto(MemorySegment sqe, int fd, long bufAddr, int len, int msgFlags,
                              long destAddr, int destAddrLen)
    {
        send(sqe, fd, bufAddr, len, msgFlags);
        SqeExt.addr2(sqe, destAddr);
        SqeExt.addrLen(sqe, destAddrLen);
    }

    /** {@code IORING_OP_RECV}: receive up to {@code len} bytes into {@code bufAddr} on {@code fd}; {@code msgFlags} are recv(2) flags. */
    public static void recv(MemorySegment sqe, int fd, long bufAddr, int len, int msgFlags)
    {
        Prep.prepRw(sqe, Opcodes.RECV, fd, bufAddr, len, 0L);
        SqeExt.opFlags(sqe, msgFlags);
    }

    /**
     * {@code IORING_OP_RECV} multishot: stays armed, pulling a fresh provided buffer per datagram/segment. Sets
     * {@link OpFlags#IORING_RECV_MULTISHOT} in {@code ioprio}; requires a registered buffer group on the socket.
     */
    public static void recvMultishot(MemorySegment sqe, int fd, long bufAddr, int len, int msgFlags)
    {
        recv(sqe, fd, bufAddr, len, msgFlags);
        Sqe.ioprio(sqe, OpFlags.IORING_RECV_MULTISHOT);
    }

    /** {@code IORING_OP_SENDMSG}: {@code msghdrAddr} points at a {@code struct msghdr}; {@code len} is fixed at 1. */
    public static void sendmsg(MemorySegment sqe, int fd, long msghdrAddr, int msgFlags)
    {
        Prep.prepRw(sqe, Opcodes.SENDMSG, fd, msghdrAddr, 1, 0L);
        SqeExt.opFlags(sqe, msgFlags);
    }

    /** {@code IORING_OP_RECVMSG}: {@code msghdrAddr} points at a {@code struct msghdr}; {@code len} is fixed at 1. */
    public static void recvmsg(MemorySegment sqe, int fd, long msghdrAddr, int msgFlags)
    {
        Prep.prepRw(sqe, Opcodes.RECVMSG, fd, msghdrAddr, 1, 0L);
        SqeExt.opFlags(sqe, msgFlags);
    }

    /**
     * {@code IORING_OP_POLL_ADD}: arm a one-shot poll on {@code fd} for the epoll event mask {@code pollMask}, written
     * to {@code poll32_events}. On little-endian hosts (aarch64/x86-64, the only targets) the mask is stored verbatim;
     * a big-endian port would word-swap it as liburing's {@code __io_uring_prep_poll_mask} does.
     */
    public static void pollAdd(MemorySegment sqe, int fd, int pollMask)
    {
        Prep.prepRw(sqe, Opcodes.POLL_ADD, fd, 0L, 0, 0L);
        SqeExt.opFlags(sqe, pollMask);
    }

    /**
     * {@code IORING_OP_POLL_ADD} multishot: like {@link #pollAdd} but re-arms, emitting a CQE (flagged
     * {@code CQE_F_MORE}) on every readiness edge. Sets {@link OpFlags#IORING_POLL_ADD_MULTI} in {@code len}.
     */
    public static void pollMultishot(MemorySegment sqe, int fd, int pollMask)
    {
        pollAdd(sqe, fd, pollMask);
        Sqe.len(sqe, OpFlags.IORING_POLL_ADD_MULTI);
    }

    /** {@code IORING_OP_POLL_REMOVE}: cancel the poll whose SQE carried {@code userData} (matched via {@code addr}). */
    public static void pollRemove(MemorySegment sqe, long userData)
    {
        Prep.prepRw(sqe, Opcodes.POLL_REMOVE, -1, 0L, 0, 0L);
        Sqe.addr(sqe, userData);
    }

    /**
     * {@code IORING_OP_POLL_REMOVE} as an update: re-target the poll matched by {@code oldUserData}, optionally
     * replacing its event mask ({@code pollMask}) and/or {@code user_data} ({@code newUserData}) per the
     * {@code IORING_POLL_UPDATE_*} bits in {@code updateFlags}.
     */
    public static void pollUpdate(MemorySegment sqe, long oldUserData, long newUserData, int pollMask, int updateFlags)
    {
        Prep.prepRw(sqe, Opcodes.POLL_REMOVE, -1, 0L, updateFlags, newUserData);
        Sqe.addr(sqe, oldUserData);
        SqeExt.opFlags(sqe, pollMask);
    }

    /** {@code IORING_OP_SHUTDOWN}: shutdown(2) on {@code fd}; {@code how} (e.g. {@code SHUT_RDWR}) rides in {@code len}. */
    public static void shutdown(MemorySegment sqe, int fd, int how)
    {
        Prep.prepRw(sqe, Opcodes.SHUTDOWN, fd, 0L, how, 0L);
    }

    /**
     * {@code IORING_OP_SOCKET}: socket(2). {@code domain} rides in {@code fd}, {@code protocol} in {@code len},
     * {@code type} in {@code off}, and the (currently reserved) creation {@code flags} in {@code rw_flags}.
     */
    public static void socket(MemorySegment sqe, int domain, int type, int protocol, int flags)
    {
        Prep.prepRw(sqe, Opcodes.SOCKET, domain, 0L, protocol, type);
        SqeExt.opFlags(sqe, flags);
    }

    /**
     * {@code IORING_OP_SOCKET} into a registered-file slot: like {@link #socket} but the new socket is installed as a
     * fixed descriptor at {@code fileIndex} (or {@link TargetFixedFile#ALLOC} to auto-allocate).
     */
    public static void socketDirect(MemorySegment sqe, int domain, int type, int protocol, int fileIndex, int flags)
    {
        socket(sqe, domain, type, protocol, flags);
        TargetFixedFile.set(sqe, fileIndex);
    }
}
