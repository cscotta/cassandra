All struct sizes and offsets confirmed empirically via gcc on this machine. I now have complete ground truth. Here is the reference document.

---

# io_uring Kernel ABI Reference for a JDK 25 FFM Binding

**Ground truth for this document:** `/usr/include/linux/io_uring.h` (29,777 bytes, dated 2025-06-23) on this machine, running kernel **`7.0.13-400.asahi.fc44.aarch64+16k`** (Fedora Asahi Remix on Apple Silicon, aarch64, 16 KiB base pages), liburing **2.13**. All struct sizes/offsets below were **verified by compiling against this header** (`gcc offsetof/sizeof` probe), not recalled. This is a bleeding-edge header — it exposes opcodes through `IORING_OP_URING_CMD128` (63) and features through `IORING_FEAT_NO_IOWAIT` that will NOT exist on the RHEL 9 / Ubuntu 22.04 kernels Cassandra must also run on. **The binding must probe features/opcodes at runtime, never assume the compile-host header.**

> **Note on 16 KiB pages:** this Asahi kernel uses a 16 KiB base page (`getconf PAGE_SIZE` = 16384), not 4096. `mmap` offsets and O_DIRECT logical-block alignment (512/4096) are independent of this, but any `mmap`-length rounding must use the runtime page size, not a hardcoded 4096.

---

## 1. The three syscalls

io_uring has **no glibc wrappers** — you must invoke the raw syscalls. Syscall numbers are identical on x86-64 and aarch64 (aarch64 uses the generic table `asm-generic/unistd.h`, confirmed on this box):

| Syscall | x86-64 | aarch64 | C prototype |
|---|---|---|---|
| `io_uring_setup` | 425 | 425 | `int io_uring_setup(u32 entries, struct io_uring_params *p)` |
| `io_uring_enter` | 426 | 426 | `int io_uring_enter(u32 fd, u32 to_submit, u32 min_complete, u32 flags, const void *arg, size_t argsz)` |
| `io_uring_register`| 427 | 427 | `int io_uring_register(u32 fd, u32 opcode, void *arg, u32 nr_args)` |

Historically `io_uring_enter` was the 5-arg form `(fd, to_submit, min_complete, flags, sigset_t*, size_t sigsz)`; since kernel 5.11 (`IORING_FEAT_EXT_ARG`), when `IORING_ENTER_EXT_ARG` is set the 5th/6th args become `struct io_uring_getevents_arg *` and `sizeof(that struct)` (=24 B, verified). Arity is fixed at 6 either way.

**FFM binding shape.** Bind libc `syscall` (variadic) via `Linker.nativeLinker()` + default `SymbolLookup`. Because `syscall(2)` is variadic, you need **one `FunctionDescriptor` per fixed arity** with `Linker.Option.firstVariadicArg(1)` (the syscall number is arg 0, non-variadic). Recommended: three purpose-built downcall handles, each prepending the per-arch `long` syscall number:

- setup: `(long nr, int entries, MemorySegment params) -> int`
- enter: `(long nr, int fd, int to_submit, int min_complete, int flags, MemorySegment arg, long argsz) -> int`
- register: `(long nr, int fd, int opcode, MemorySegment arg, int nr_args) -> int`

Attach `Linker.Option.captureCallState("errno")` (JDK 22+) to every handle and read `errno` from the capture segment when the return is `< 0`. Also bind `mmap`/`munmap` (`mmap` is NOT variadic — fixed 6-arg) for ring memory.

**`io_uring_enter` flags** (`enter(2)`, offset from header lines 593–602):

| Flag | Value | Meaning |
|---|---|---|
| `IORING_ENTER_GETEVENTS` | `1<<0` | Wait until `min_complete` CQEs are available before returning (the blocking reap path). |
| `IORING_ENTER_SQ_WAKEUP` | `1<<1` | Wake a parked SQPOLL kernel thread. Set only when `sq flags & IORING_SQ_NEED_WAKEUP`. |
| `IORING_ENTER_SQ_WAIT` | `1<<2` | Wait for SQPOLL thread to consume SQ entries (SQ full). |
| `IORING_ENTER_EXT_ARG` | `1<<3` | 5th/6th args are `io_uring_getevents_arg*`/size — enables a wait timeout without a separate timeout SQE (kernel ≥ 5.11). |
| `IORING_ENTER_REGISTERED_RING` | `1<<4` | `fd` is a registered ring index (from `IORING_REGISTER_RING_FDS`), not a real fd — saves the fdget/fdput per enter (kernel ≥ 5.18). |
| `IORING_ENTER_ABS_TIMER` | `1<<5` | `getevents_arg` timeout is absolute (newer). |
| `IORING_ENTER_EXT_ARG_REG` | `1<<6` | `arg` is an index into a registered wait region (`io_uring_reg_wait`). |
| `IORING_ENTER_NO_IOWAIT` | `1<<7` | Don't account the wait as iowait (avoids inflating load average / cpufreq). |

**errno cases** (all three syscalls):
- `EAGAIN` — resource temporarily unavailable; for reads a non-blocking op would block (retryable).
- `EINTR` — interrupted by signal while waiting for events; **retry the enter** (don't treat as failure).
- `EBUSY` — SQ ring is full / kernel couldn't consume SQEs (e.g. IOPOLL backlog, or overflow with `NODROP`); back off and reap CQEs before resubmitting.
- `ENOSYS` — io_uring not compiled in or kernel too old → **feature unavailable, fall back to `standard`/`direct`.**
- `EPERM` — blocked by seccomp (default Docker profile disables io_uring on ≥5.10) or a `RESTRICTIONS` policy → **treat as unavailable, fall back.**
- Others worth handling: `EFAULT` (bad pointer — a binding bug), `EINVAL` (bad flags/args), `ENOMEM` (setup), `EMFILE`/`ENFILE` (fd limits), `EOPNOTSUPP`.

---

## 2. Exact memory layouts (verified sizes/offsets)

All offsets below are **compiler-verified on this machine**. Use these directly to build `MemoryLayout.structLayout(...)` with `paddingLayout(...)` where unions leave gaps, and derive `VarHandle`s via `layout.varHandle(groupElement("field"))` (or fixed `long` offsets, which are simpler for unions).

### `struct io_uring_params` — 120 B (`sq_off`@40, `cq_off`@80)
```
off  type   field
0    u32    sq_entries
4    u32    cq_entries
8    u32    flags            // IORING_SETUP_*
12   u32    sq_thread_cpu    // valid iff SQ_AFF
16   u32    sq_thread_idle
20   u32    features         // OUT: IORING_FEAT_*  <-- read after setup
24   u32    wq_fd            // valid iff ATTACH_WQ
28   u32    resv[3]          // 12 B
40   struct io_sqring_offsets sq_off   // 40 B
80   struct io_cqring_offsets cq_off   // 40 B
```
`entries`, `flags`, `sq_thread_cpu/idle`, `wq_fd` are IN; everything else is filled by the kernel. **Read `features`@20 after setup and cache it.**

### `struct io_sqring_offsets` — 40 B
```
0  u32 head        // kernel-owned index (you read w/ acquire)
4  u32 tail        // app-owned (you write w/ release)
8  u32 ring_mask   // = sq_entries-1; mask indices
12 u32 ring_entries
16 u32 flags       // IORING_SQ_* (NEED_WAKEUP / CQ_OVERFLOW / TASKRUN)
20 u32 dropped     // count of invalid SQEs dropped
24 u32 array       // byte offset within SQ mmap to the sq_array[] index table
28 u32 resv1
32 u64 user_addr   // used with SETUP_NO_MMAP
```
These are **byte offsets into the SQ-ring mmap** (except `user_addr`). E.g. the live SQ tail lives at `sq_ring_base + sq_off.tail`.

### `struct io_cqring_offsets` — 40 B
```
0  u32 head        // app-owned (you write w/ release after consuming)
4  u32 tail        // kernel-owned (you read w/ acquire)
8  u32 ring_mask   // = cq_entries-1
12 u32 ring_entries
16 u32 overflow    // count of overflowed (dropped) CQEs
20 u32 cqes        // byte offset within CQ mmap to the cqe[] array
24 u32 flags       // IORING_CQ_EVENTFD_DISABLED
28 u32 resv1
32 u64 user_addr
```

### `struct io_uring_sqe` — 64 B (SQE128 = 128 B via `IORING_SETUP_SQE128`)
Verified offsets. Fields are heavily unionized; for a read binding you only touch a handful:
```
0   u8   opcode           // IORING_OP_*
1   u8   flags            // IOSQE_*
2   u16  ioprio           // also multishot/recvsend flags
4   s32  fd               // or fixed-file index if IOSQE_FIXED_FILE
8   u64  off              // file offset  (union: addr2, cmd_op/__pad1)
16  u64  addr             // buffer ptr / iovec ptr (union: splice_off_in, level/optname)
24  u32  len              // byte count, or #iovecs for READV
28  u32  rw_flags         // op-specific flags union (fsync_flags, timeout_flags, ...)
32  u64  user_data        // opaque correlation token, echoed in CQE
40  u16  buf_index        // fixed-buffer index (union w/ buf_group), __packed
42  u16  personality
44  s32  splice_fd_in     // union: file_index (u32), addr_len, write_stream...
48  u64  addr3            // union: attr_ptr, optval; cmd[] for SQE128 starts @48
56  u64  __pad2[1]        // (part of the addr3 union block)
```
For **`IORING_OP_READ`/`WRITE`** (single contiguous buffer): set `opcode`, `fd`, `off` (file offset), `addr` (buffer address), `len` (bytes), `user_data`; leave `rw_flags`=0. For **`READV`/`WRITEV`**: `addr` = pointer to `iovec[]`, `len` = iovec count. For **`READ_FIXED`/`WRITE_FIXED`**: `addr` = target address *inside* a registered buffer, `len` = bytes, `buf_index`@40 = registered-buffer slot. **`rw_flags` carries `RWF_*`** (e.g. `RWF_HIPRI`, `RWF_NOWAIT`, `RWF_APPEND`).

Recommended `VarHandle` set (offset-based, using `ValueLayout.JAVA_*_UNALIGNED` so field access never trips alignment on the packed struct): `SQE_OPCODE (JAVA_BYTE@0)`, `SQE_FLAGS (@1)`, `SQE_IOPRIO (JAVA_SHORT@2)`, `SQE_FD (JAVA_INT@4)`, `SQE_OFF (JAVA_LONG@8)`, `SQE_ADDR (JAVA_LONG@16)`, `SQE_LEN (JAVA_INT@24)`, `SQE_RW_FLAGS (JAVA_INT@28)`, `SQE_USER_DATA (JAVA_LONG@32)`, `SQE_BUF_INDEX (JAVA_SHORT@40)`. Zero the whole 64 B (`MemorySegment.fill(0)`) before filling to avoid stale union bytes.

### `struct io_uring_cqe` — 16 B (CQE32 = 32 B via `IORING_SETUP_CQE32`)
```
0  u64 user_data   // echoes sqe.user_data
8  s32 res         // >=0 result (bytes transferred), or -errno on failure
12 u32 flags       // IORING_CQE_F_*
16 u64 big_cqe[]   // present only with CQE32 (16 extra bytes)
```
**`res < 0` ⇒ error, value is `-errno`** (e.g. `-EIO` = -5). For reads, `res` = bytes read (may be a **short read** < requested `len`, especially at EOF — the binding must loop/retry the remainder, exactly like a partial `pread`).

`IORING_CQE_F_*` flags:
| Flag | Value | Meaning |
|---|---|---|
| `IORING_CQE_F_BUFFER` | `1<<0` | Upper 16 bits of `flags` hold the provided-buffer ID (`>> IORING_CQE_BUFFER_SHIFT` = 16). |
| `IORING_CQE_F_MORE` | `1<<1` | More CQEs will come for this SQE (multishot). |
| `IORING_CQE_F_SOCK_NONEMPTY`| `1<<2` | Socket has more data (recv). |
| `IORING_CQE_F_NOTIF` | `1<<3` | This is a zero-copy notification CQE. |
| `IORING_CQE_F_BUF_MORE` | `1<<4` | Incremental-consumption buffer still in use. |
| `IORING_CQE_F_SKIP` | `1<<5` | Filler CQE — ignore it. |
| `IORING_CQE_F_32` | `1<<15`| This is a 32 B big-CQE (mixed-CQE mode). |
`IORING_CQE_BUFFER_SHIFT = 16`.

### `struct iovec` — 16 B (from `<sys/uio.h>`)
```
0  void*  iov_base   // JAVA_LONG (pointer)
8  size_t iov_len    // JAVA_LONG
```

### `struct __kernel_timespec` — 16 B
```
0  s64 tv_sec        // __kernel_time64_t
8  s64 tv_nsec       // long long — 64-bit even on 32-bit arch
```
Note: this is **NOT** libc `struct timespec` (whose `tv_nsec` is `long`). io_uring always uses the 64-bit `__kernel_timespec`. Used inside `IORING_OP_TIMEOUT` (via `addr`) and in `io_uring_getevents_arg.ts` for `EXT_ARG` waits.

---

## 3. `IORING_SETUP_*` flags (header lines 172–251)

| Flag | Value | Kernel | Meaning |
|---|---|---|---|
| `IOPOLL` | `1<<0` | 5.1 | Busy-poll completions (O_DIRECT block devices only); no interrupts. **CQEs must be reaped via `enter(GETEVENTS)`, not the ring tail.** |
| `SQPOLL` | `1<<1` | 5.1 | Kernel thread polls the SQ — submit without a syscall. Burns a CPU; pre-5.11 needs `CAP_SYS_ADMIN`. |
| `SQ_AFF` | `1<<2` | 5.1 | Pin the SQPOLL thread to `sq_thread_cpu`. |
| `CQSIZE` | `1<<3` | 5.5 | `cq_entries` in params defines CQ size (else CQ = 2× SQ). |
| `CLAMP` | `1<<4` | 5.6 | Clamp SQ/CQ sizes to kernel max instead of `EINVAL`. |
| `ATTACH_WQ` | `1<<5` | 5.6 | Share the io-wq worker pool of the ring in `wq_fd` (avoid N× worker threads). |
| `R_DISABLED` | `1<<6` | 5.10 | Create the ring disabled; must `IORING_REGISTER_ENABLE_RINGS` before use (lets you register restrictions first). |
| `SUBMIT_ALL` | `1<<7` | 5.18 | Don't stop the submit batch on the first bad SQE. |
| `COOP_TASKRUN` | `1<<8` | 5.19 | Don't force an IPI to run completion task-work; run it on natural kernel transitions. Lowers latency jitter. |
| `TASKRUN_FLAG` | `1<<9` | 5.19 | With COOP_TASKRUN, set `IORING_SQ_TASKRUN` in SQ flags when task-work is pending (so you know to enter). |
| `SQE128` | `1<<10`| 5.19 | 128 B SQEs (needed for `URING_CMD` passthrough). |
| `CQE32` | `1<<11`| 5.19 | 32 B CQEs. |
| `SINGLE_ISSUER` | `1<<12`| 6.0 | Promise only one task submits — kernel optimizes locking. Pairs with DEFER_TASKRUN. |
| `DEFER_TASKRUN` | `1<<13`| 6.1 | Defer completion task-work until the app calls `enter(GETEVENTS)`. Big latency/throughput win for a single poller thread; requires SINGLE_ISSUER. |
| `NO_MMAP` | `1<<14`| 6.5 | App supplies ring memory (huge pages) via `sq_off/cq_off.user_addr`; no `mmap` needed. |
| `REGISTERED_FD_ONLY`| `1<<15`| 6.5 | `setup` returns a registered ring index, not a real fd (implies you use `ENTER_REGISTERED_RING`). |
| `NO_SQARRAY` | `1<<16`| 6.6 | Drop the SQ index indirection array — SQEs consumed in ring order directly. Simplifies the SQ (no `sq_array` to maintain). |
| `HYBRID_IOPOLL` | `1<<17`| 6.12+ | Hybrid (sleep+poll) IOPOLL. |
| `CQE_MIXED` | `1<<18`| newest | Allow both 16 B and 32 B CQEs (32 B ones flagged `IORING_CQE_F_32`). |
| `SQE_MIXED` | `1<<19`| newest | Allow both 64 B and 128 B SQEs. |
| `SQ_REWIND` | `1<<20`| newest | Kernel ignores SQ head/tail, reads SQEs from index 0; requires `NO_SQARRAY`, incompatible with `SQPOLL`. |

**Recommendation for Cassandra's read path:** default plain (no SQPOLL). On kernels ≥ 6.1, strongly consider `SINGLE_ISSUER | DEFER_TASKRUN` for the one-poller-thread-per-ring model (§7 of the plan) — it gives the best latency and cuts spurious task-work. Gate both behind a feature/version probe; both are absent on RHEL 9 (5.14) / Ubuntu 22.04 (5.15).

---

## 4. `IORING_FEAT_*` flags (returned in `params.features`, lines 623–640)

| Feature | Value | Kernel | Meaning / why the binding cares |
|---|---|---|---|
| `SINGLE_MMAP` | `1<<0` | 5.4 | SQ and CQ rings share **one** mmap → 2 mmaps total (ring + SQES) instead of 3. |
| `NODROP` | `1<<1` | 5.5 | CQEs are never silently dropped; overflow is backlogged. Enter may then return `EBUSY`. |
| `SUBMIT_STABLE` | `1<<2` | 5.5 | SQE data is fully consumed at submit — safe to reuse/overwrite the SQE (and stack iovecs) immediately after `enter`. |
| `RW_CUR_POS` | `1<<3` | 5.6 | `off = -1` means "use the fd's current file position" for read/write. |
| `CUR_PERSONALITY` | `1<<4` | 5.6 | Ops use the creds of the submitting task by default. |
| `FAST_POLL` | `1<<5` | 5.7 | Internal poll-based async for read/write — huge for network, relevant for pollable fds. |
| `POLL_32BITS` | `1<<6` | 5.9 | `POLL_ADD` accepts 32-bit epoll flags via `poll32_events`. |
| `SQPOLL_NONFIXED` | `1<<7` | 5.11 | SQPOLL works without registered files. |
| `EXT_ARG` | `1<<8` | 5.11 | `enter` accepts `io_uring_getevents_arg` (wait timeout without a timeout SQE). |
| `NATIVE_WORKERS` | `1<<9` | 5.12 | io-wq uses native kernel worker threads (not kthreads). |
| `RSRC_TAGS` | `1<<10`| 5.13 | Tagged/updatable registered buffers & files (`REGISTER_*2`). |
| `CQE_SKIP` | `1<<11`| 5.17 | `IOSQE_CQE_SKIP_SUCCESS` honored (suppress CQE on success). |
| `LINKED_FILE` | `1<<12`| 5.17 | Correct file assignment for linked SQEs (needed for reliable `IOSQE_IO_LINK` on files). |
| `REG_REG_RING` | `1<<13`| 6.3 | Can register the ring fd using an already-registered ring (`ENTER_REGISTERED_RING` for register too). |
| `RECVSEND_BUNDLE`| `1<<14`| 6.10 | Bundled provided-buffer send/recv. |
| `MIN_TIMEOUT` | `1<<15`| 6.12 | Minimum-wait timeout for batched completions. |
| `RW_ATTR` | `1<<16`| newest | Per-IO attributes (e.g. PI/T10-DIF via `io_uring_attr_pi`). |
| `NO_IOWAIT` | `1<<17`| newest | Supports `IORING_ENTER_NO_IOWAIT`. |

The binding **must record `features` at setup** and gate optional paths on it: use `SINGLE_MMAP` to decide 2-vs-3 mmaps; require nothing beyond `SINGLE_MMAP`+`IORING_OP_READ` (kernel ≥ 5.6) for the baseline read path; use `EXT_ARG` for a poll-with-timeout loop; use `SUBMIT_STABLE` to know when iovec/SQE reuse is safe.

---

## 5. Full opcode list `IORING_OP_*` (header lines 253–322, enum ordinal = opcode number)

| # | Opcode | Kernel | One-line semantics |
|---|---|---|---|
| 0 | `NOP` | 5.1 | No-op; completes immediately (probe/keepalive). |
| 1 | `READV` | 5.1 | Vectored pread (`addr`→iovec[], `len`=count). |
| 2 | `WRITEV` | 5.1 | Vectored pwrite. |
| 3 | `FSYNC` | 5.1 | fsync/fdatasync (`fsync_flags & IORING_FSYNC_DATASYNC`). |
| 4 | `READ_FIXED` | 5.1 | pread into a **registered** buffer (`buf_index`). |
| 5 | `WRITE_FIXED`| 5.1 | pwrite from a registered buffer. |
| 6 | `POLL_ADD` | 5.1 | Arm a poll on fd; multishot via `IORING_POLL_ADD_MULTI`. |
| 7 | `POLL_REMOVE`| 5.1 | Cancel a POLL_ADD (match on `addr`=user_data). |
| 8 | `SYNC_FILE_RANGE` | 5.2 | sync_file_range(2). |
| 9 | `SENDMSG` | 5.3 | sendmsg(2). |
| 10| `RECVMSG` | 5.3 | recvmsg(2). |
| 11| `TIMEOUT` | 5.4 | Timeout that fires a CQE (`addr`→`__kernel_timespec`); count- or time-based. |
| 12| `TIMEOUT_REMOVE`/UPDATE | 5.5/5.11 | Cancel or (with `IORING_TIMEOUT_UPDATE`) re-arm a timeout. |
| 13| `ACCEPT` | 5.5 | accept4(2); multishot via `IORING_ACCEPT_MULTISHOT`. |
| 14| `ASYNC_CANCEL`| 5.5 | Cancel an in-flight request by `user_data`/fd/op (`cancel_flags`). |
| 15| `LINK_TIMEOUT`| 5.5 | Timeout attached to the *previous linked* SQE (cancels it on expiry). |
| 16| `CONNECT` | 5.5 | connect(2). |
| 17| `FALLOCATE` | 5.6 | fallocate(2). |
| 18| `OPENAT` | 5.6 | openat(2); can install a fixed-file slot via `file_index`. |
| 19| `CLOSE` | 5.6 | close(2) (or close a fixed-file slot). |
| 20| `FILES_UPDATE`| 5.6 | Update registered-file table inline. |
| 21| `STATX` | 5.6 | statx(2). |
| 22| `READ` | 5.6 | **pread into a single contiguous buffer** (`addr`,`len`,`off`). *Primary op for Cassandra.* |
| 23| `WRITE` | 5.6 | pwrite from a single contiguous buffer. |
| 24| `FADVISE` | 5.6 | posix_fadvise(2). |
| 25| `MADVISE` | 5.6 | madvise(2). |
| 26| `SEND` | 5.6 | send(2). |
| 27| `RECV` | 5.6 | recv(2); multishot via `IORING_RECV_MULTISHOT`. |
| 28| `OPENAT2` | 5.6 | openat2(2) (`how` struct via `addr`). |
| 29| `EPOLL_CTL` | 5.6 | epoll_ctl(2). |
| 30| `SPLICE` | 5.7 | splice(2). |
| 31| `PROVIDE_BUFFERS` | 5.7 | Donate a group of buffers for `BUFFER_SELECT` (classic, pre-buf-ring). |
| 32| `REMOVE_BUFFERS`| 5.7 | Remove provided buffers. |
| 33| `TEE` | 5.8 | tee(2). |
| 34| `SHUTDOWN` | 5.11 | shutdown(2). |
| 35| `RENAMEAT` | 5.11 | renameat2(2). |
| 36| `UNLINKAT` | 5.11 | unlinkat(2). |
| 37| `MKDIRAT` | 5.15 | mkdirat(2). |
| 38| `SYMLINKAT` | 5.15 | symlinkat(2). |
| 39| `LINKAT` | 5.15 | linkat(2). |
| 40| `MSG_RING` | 5.18 | Post a message/CQE to another ring (`IORING_MSG_DATA`/`SEND_FD`). |
| 41–44| `FSETXATTR`/`SETXATTR`/`FGETXATTR`/`GETXATTR` | 5.19 | xattr ops. |
| 45| `SOCKET` | 5.19 | socket(2). |
| 46| `URING_CMD` | 5.19 | Passthrough command (e.g. NVMe); needs SQE128. |
| 47| `SEND_ZC` | 6.0 | Zero-copy send; emits a `NOTIF` CQE. |
| 48| `SENDMSG_ZC` | 6.1 | Zero-copy sendmsg. |
| 49| `READ_MULTISHOT` | 6.7 | Multishot read into provided buffers (repeated CQEs). |
| 50| `WAITID` | 6.7 | waitid(2). |
| 51–53| `FUTEX_WAIT`/`WAKE`/`WAITV` | 6.7 | futex ops. |
| 54| `FIXED_FD_INSTALL` | 6.8 | Turn a fixed-file slot into a real installed fd (`install_fd_flags`). |
| 55| `FTRUNCATE` | 6.9 | ftruncate(2). |
| 56| `BIND` | 6.11 | bind(2). |
| 57| `LISTEN` | 6.11 | listen(2). |
| 58| `RECV_ZC` | 6.11 | Zero-copy receive (zcrx). |
| 59| `EPOLL_WAIT` | 6.13+ | epoll_wait(2). |
| 60| `READV_FIXED`| newest | Vectored read into registered buffers. |
| 61| `WRITEV_FIXED`| newest | Vectored write from registered buffers. |
| 62| `PIPE` | newest | pipe(2). |
| 63| `NOP128`/`URING_CMD128` | newest | 128 B-SQE variants. |

Cassandra needs exactly: **`READ` (22)** primary, **`READV` (1)** for scatter/gather, **`READ_FIXED` (4)**/**`WRITE_FIXED` (5)** and **`READV_FIXED` (60)** for the registered-buffer phase, `FSYNC`/`WRITE`/`WRITEV` if write-path is ever added, and `NOP` (0) for the availability probe. Everything else is out of scope. **Do not trust opcode numbers ≥ 34 to exist** — probe via `IORING_REGISTER_PROBE`.

---

## 6. SQE flags `IOSQE_*` (lines 141–167) and ordering semantics

Bit definitions (from the `enum ..._bit`, so value = `1<<bit`):
| Flag | Value | Meaning |
|---|---|---|
| `IOSQE_FIXED_FILE` | `1<<0` | `sqe.fd` is an index into the registered-file table, not a real fd. |
| `IOSQE_IO_DRAIN` | `1<<1` | This SQE waits until **all previously submitted** SQEs complete before starting (full pipeline barrier). |
| `IOSQE_IO_LINK` | `1<<2` | Link to the **next** SQE: it won't start until this one completes **successfully**. A chain ends at the first SQE without the flag. |
| `IOSQE_IO_HARDLINK`| `1<<3` | Like LINK but the chain continues even if this SQE fails (errors don't sever the chain). |
| `IOSQE_ASYNC` | `1<<4` | Force async (io-wq) execution instead of trying inline/nonblocking first. |
| `IOSQE_BUFFER_SELECT`| `1<<5`| Pick a buffer from the group in `buf_group` at execution time; buffer ID returned in CQE (`F_BUFFER`). |
| `IOSQE_CQE_SKIP_SUCCESS`| `1<<6`| Don't post a CQE if this SQE succeeds (only on failure). Requires `IORING_FEAT_CQE_SKIP`. |

**Ordering guarantees (critical for a correct binding):**
- **By default there is NO ordering** — submitted SQEs may complete in any order and execute concurrently. This is exactly what Cassandra's parallel read path wants; correlate purely by `user_data`, never by submission order.
- **`IO_LINK`** creates a sequential chain: SQE *N+1* starts only after *N* completes OK. If *N* fails (incl. short read counting as "not full success" for some ops), the rest of the chain is **cancelled with `-ECANCELED`**. `HARDLINK` keeps the chain alive through failures. Links are only ordered *within* the chain; independent chains still run concurrently.
- **`IO_DRAIN`** is a heavyweight barrier against **all** prior in-flight I/O in the ring — expensive, avoid on the hot path.
- **`LINK_TIMEOUT`** must immediately follow the SQE it guards, itself linked.
- With `IORING_FEAT_LINKED_FILE`, file assignment for linked ops is resolved correctly at submit; without it, linking file ops is racy.

For Cassandra's independent chunk reads: **submit unlinked, no drain**, one `user_data` per read, reap out-of-order. Links are only useful for the readahead/prefetch strategy if a strict order is required (usually not).

---

## 7. Registration `IORING_REGISTER_*` (enum lines 645–724)

Opcode numbers (pass as `io_uring_register`'s 2nd arg):
```
0  REGISTER_BUFFERS         1  UNREGISTER_BUFFERS
2  REGISTER_FILES           3  UNREGISTER_FILES
4  REGISTER_EVENTFD         5  UNREGISTER_EVENTFD
6  REGISTER_FILES_UPDATE    7  REGISTER_EVENTFD_ASYNC
8  REGISTER_PROBE           9  REGISTER_PERSONALITY
10 UNREGISTER_PERSONALITY   11 REGISTER_RESTRICTIONS
12 ENABLE_RINGS
13 REGISTER_FILES2          14 REGISTER_FILES_UPDATE2
15 REGISTER_BUFFERS2        16 REGISTER_BUFFERS_UPDATE
17 REGISTER_IOWQ_AFF        18 UNREGISTER_IOWQ_AFF
19 REGISTER_IOWQ_MAX_WORKERS
20 REGISTER_RING_FDS        21 UNREGISTER_RING_FDS
22 REGISTER_PBUF_RING       23 UNREGISTER_PBUF_RING
24 REGISTER_SYNC_CANCEL
25 REGISTER_FILE_ALLOC_RANGE
26 REGISTER_PBUF_STATUS
27 REGISTER_NAPI            28 UNREGISTER_NAPI
29 REGISTER_CLOCK           30 REGISTER_CLONE_BUFFERS
31 REGISTER_SEND_MSG_RING   32 REGISTER_ZCRX_IFQ
33 REGISTER_RESIZE_RINGS    34 REGISTER_MEM_REGION
35 REGISTER_QUERY           36 REGISTER_ZCRX_CTRL   37 REGISTER_BPF_FILTER
// OR this into the opcode to use a registered ring fd:
IORING_REGISTER_USE_REGISTERED_RING = 1U<<31
```

Relevant to Cassandra:
- **`REGISTER_BUFFERS` (0)** / **`REGISTER_BUFFERS2` (15, tagged, kernel ≥ 5.13)** — pin an array of `struct iovec` (base+len) describing `BufferPool` slabs. SQEs then use **`READ_FIXED`/`WRITE_FIXED`** with `buf_index` + an address *inside* a registered region. Removes per-I/O page get/put pinning. **Charges `RLIMIT_MEMLOCK`** — check `getrlimit(RLIMIT_MEMLOCK)`; on `ENOMEM`/`EPERM` fall back to unregistered `READ`. Prefer `BUFFERS2` with `IORING_RSRC_REGISTER_SPARSE` (`1<<0`) so slots can be filled/updated later via `BUFFERS_UPDATE` (16).
- **`REGISTER_FILES` (2)** / **`REGISTER_FILES2` (13, tagged, kernel ≥ 5.5/5.13)** — register SSTable fds; use `IOSQE_FIXED_FILE` with the slot index in `sqe.fd`. `io_uring_rsrc_register{nr, flags, resv2, data(ptr to fd array), tags}`. `IORING_REGISTER_FILES_SKIP = -2` skips a slot on update; `IORING_FILE_INDEX_ALLOC = ~0U` auto-allocates a slot (with `FILE_ALLOC_RANGE`).
- **`REGISTER_PROBE` (8)** — fill a `struct io_uring_probe` (`last_op`, `ops_len`, then `io_uring_probe_op ops[]` each `{op, resv, flags, resv2}`); an op is supported iff `flags & IO_URING_OP_SUPPORTED (1<<0)`. **This is the correct runtime opcode-availability check** — use it instead of trusting the compile header.
- **`REGISTER_RING_FDS` (20, kernel ≥ 5.18)** — register the ring fd itself; subsequent `enter` calls pass the index + `IORING_ENTER_REGISTERED_RING` to skip fd lookup. Cheap latency win for the poller.
- **`REGISTER_ENABLE_RINGS` (12)** — with `SETUP_R_DISABLED`, enables the ring after `REGISTER_RESTRICTIONS`.
- **`REGISTER_PBUF_RING` (22)** / **`PBUF_STATUS` (26)** — set up a `io_uring_buf_ring` provided-buffer ring (see §9).
- **`REGISTER_SYNC_CANCEL` (24)** — synchronous cancel-by-key for shutdown drain.
- **`REGISTER_FILE_ALLOC_RANGE` (25)** — reserve a slot range for auto-allocation.

`struct io_uring_rsrc_register` (buffers2/files2): `{u32 nr; u32 flags; u64 resv2; u64 data; u64 tags}` (`data`/`tags` are `__aligned_u64`). `struct io_uring_rsrc_update2`: `{u32 offset; u32 resv; u64 data; u64 tags; u32 nr; u32 resv2}`.

---

## 8. mmap layout and memory barriers (the correctness core)

**Magic mmap offsets** (lines 544–549):
```
IORING_OFF_SQ_RING   = 0x0
IORING_OFF_CQ_RING   = 0x8000000
IORING_OFF_SQES      = 0x10000000
IORING_OFF_PBUF_RING = 0x80000000   // | (bgid << 16) for provided-buffer rings
```
All mmaps: `PROT_READ|PROT_WRITE`, `MAP_SHARED|MAP_POPULATE`, `fd` = ring fd, `offset` = one of the above.

**Sizing the mmaps:**
- SQ ring bytes = `sq_off.array + sq_entries * sizeof(u32)` (the sq_array is a `u32` index table). Without `NO_SQARRAY`, `sq_off.array` points at this table inside the SQ mmap.
- CQ ring bytes = `cq_off.cqes + cq_entries * sizeof(io_uring_cqe)` (16 B, or 32 B with CQE32).
- SQES mmap bytes = `sq_entries * sizeof(io_uring_sqe)` (64 B, or 128 B with SQE128), at `IORING_OFF_SQES`.

**Single vs double mmap:** if `params.features & IORING_FEAT_SINGLE_MMAP` (kernel ≥ 5.4, effectively always today), the SQ and CQ rings live in the **same** mapping — do **one** mmap at `IORING_OFF_SQ_RING` sized to `max(sq_ring_bytes, cq_ring_bytes)` and derive both ring bases from it, plus a **second** mmap for the SQES. Without the feature: three separate mmaps. Wrap each returned address with `MemorySegment.ofAddress(a).reinterpret(len, arena, cleanup)` where `cleanup` calls `munmap`.

**The `sq_array` indirection:** the SQE array (at `IORING_OFF_SQES`) is a *pool* of SQEs; the kernel doesn't read them in order. To submit SQE at pool slot `i`, you must write `i` into `sq_array[sq_tail & sq_ring_mask]` and *then* advance `sq_tail`. (With `SETUP_NO_SQARRAY`, kernel ≥ 6.6, this table is gone and SQEs are consumed in ring order — simpler; gate on version.) Typical single-threaded flow reuses slot = `tail & mask` so `sq_array[i] = i`.

**Ring field access = shared memory with the kernel. Exact barrier requirements (mirror liburing `smp_load_acquire`/`smp_store_release`):**

| Field | Owner | Your access | Barrier |
|---|---|---|---|
| SQ `tail` | you | **store** after filling SQE + sq_array | **`setRelease`** — publishes SQE contents before the kernel sees the new tail. |
| SQ `head` | kernel | **load** (to compute free space) | **`getAcquire`** — see kernel's consumption before reusing SQEs. |
| SQ `flags`| kernel | load (NEED_WAKEUP / CQ_OVERFLOW / TASKRUN) | acquire (or plain volatile load is acceptable for flags). |
| SQ `dropped` | kernel | load (diagnostic) | plain/acquire. |
| CQ `tail` | kernel | **load** to find new CQEs | **`getAcquire`** — ensures CQE `res`/`user_data`/`flags` written by kernel are visible after you observe the tail. |
| CQ `head` | you | **store** after consuming CQEs | **`setRelease`** — tells kernel the slots are free only after you've read them. |
| CQ `overflow`| kernel | load (diagnostic) | plain/acquire. |
| SQE/CQE payload | — | read/write | ordered by the tail/head release/acquire above; individual field VarHandles can be plain. |

In JDK FFM terms: obtain these via `layout.varHandle(...)` and use `VarHandle.getAcquire(seg, offset)` / `VarHandle.setRelease(seg, offset, val)`. Equivalently `MemorySegment.get/set` with an explicit `VarHandle` acquire/release mode. **Getting these two release/two acquire points wrong = silent data corruption or missed completions**, so encode them as named helper methods (`publishSqTail`, `loadCqTail`, `advanceCqHead`, `loadSqHead`) and never open-code them.

**SQ flags to check (lines 569–571):**
- `IORING_SQ_NEED_WAKEUP (1<<0)` — SQPOLL thread parked; you must `enter(SQ_WAKEUP)` to wake it. (Only relevant with SQPOLL.)
- `IORING_SQ_CQ_OVERFLOW (1<<1)` — CQ overflowed; `enter(GETEVENTS)` to flush the backlog.
- `IORING_SQ_TASKRUN (1<<2)` — pending task-work; enter the kernel to run it (with TASKRUN_FLAG).

**CQ flags:** `IORING_CQ_EVENTFD_DISABLED (1<<0)`.

---

## 9. Provided buffers, buffer rings, multishot

- **Classic provided buffers:** `IORING_OP_PROVIDE_BUFFERS` (31) donates a group; a read/recv with `IOSQE_BUFFER_SELECT` + `sqe.buf_group` picks one at execution time; the CQE has `IORING_CQE_F_BUFFER` set and the **buffer ID in `cqe.flags >> IORING_CQE_BUFFER_SHIFT` (>>16)**.
- **Buffer rings (faster, kernel ≥ 5.19):** register via `IORING_REGISTER_PBUF_RING` (22) with `struct io_uring_buf_reg {u64 ring_addr; u32 ring_entries; u16 bgid; u16 flags; u32 min_left; u32 resv[5]}`. The ring is an array of `struct io_uring_buf {u64 addr; u32 len; u16 bid; u16 resv}` (16 B). Its tail is overlaid on the first entry's `resv` field (`struct io_uring_buf_ring` union, lines 857–871) — you publish new buffers by writing entries and advancing that `tail` with a **release** store. Flags: `IOU_PBUF_RING_MMAP (1)` (kernel allocates, you mmap at `IORING_OFF_PBUF_RING | (bgid<<16)`), `IOU_PBUF_RING_INC (2)` (incremental consumption; CQE sets `IORING_CQE_F_BUF_MORE`).
- **Multishot ops** (`ACCEPT_MULTISHOT`, `RECV_MULTISHOT`, `POLL_ADD_MULTI`, `READ_MULTISHOT` op 49): one SQE yields **repeated CQEs**, each flagged `IORING_CQE_F_MORE` until the last (which clears it). Each completion consumes a fresh provided buffer.

**Relevance to Cassandra:** provided buffers and multishot are **socket/streaming-oriented** and do **not** fit the positional-pread chunk-cache read model (each chunk read targets a *specific* pre-allocated `BufferPool` slice at a known offset). The binding for the read path should use **`READ`/`READ_FIXED` with an explicit `addr`**, not `BUFFER_SELECT`. Document buffer rings as out-of-scope for the disk read path; they'd only matter if io_uring were ever used for the native transport (which is Netty's job).

---

## 10. O_DIRECT alignment interaction

O_DIRECT (`ExtendedOpenOption.DIRECT`, already used by Cassandra's `direct` mode) imposes hardware alignment on **all three** of: buffer address, file offset, and length — each must be a multiple of the device **logical block size** (512 B on most, but **4096 B on many NVMe** with 4Kn or `LOGICAL_BLOCK_SIZE=4096`). io_uring does **not** relax this: `IORING_OP_READ`/`READV` on an O_DIRECT fd is subject to the same `EINVAL` on misalignment as a plain `pread`. Buffered (non-O_DIRECT) reads have no alignment requirement.

Concrete rules for the binding:
- **Offset:** the chunk offset must be block-aligned. Cassandra chunks are 64 KiB and `alignmentMask = -chunkSize`, so chunk-aligned offsets are already 4096-aligned. Any sub-chunk read must round the offset down and adjust.
- **Length:** round the requested length **up** to a block multiple before submit (pattern: `DirectThreadLocalReadAheadBuffer`), then trim `ByteBuffer.limit()` to the real byte count from `cqe.res` after completion (which for a valid read equals the padded length up to EOF; near EOF the device returns the aligned amount and you trim to logical file size).
- **Buffer address:** must be block-aligned. The `BufferPool` unit is 2 KiB — 512-aligned but **not guaranteed 4096-aligned**. For 4096-block devices, DMA buffers must come from an aligned allocator: reuse `FileUtils.getBlockSize()` + Agrona `BufferUtil.allocateDirectAligned(cap, align)` / `BitUtil.align(...)`, or teach `BufferPool` a 4096-aligned unit. **`MemorySegment.ofBuffer(bb).address()` must land on a block boundary** — assert this before submit, else the kernel returns `-EINVAL` in `cqe.res`.
- **`RWF_NOWAIT`** (in `sqe.rw_flags`) can be set to make an O_DIRECT read fail fast with `-EAGAIN` rather than blocking a worker; useful for the IOPOLL/hot path but requires retry logic.
- Registered buffers (`READ_FIXED`) must satisfy the same address alignment — register block-aligned slabs.
- **Kernel bug watch:** the plan's open question #4 (ext4 6.1.64–6.1.66 O_DIRECT corruption, `checkKernelBug1057843`) applies to O_DIRECT writes; io_uring reads route through the same `iomap`/DIO path, so keep the existing kernel-range guard active for the `io_uring` mode too.

---

## FFM implementation mapping (summary)

| ABI element | JDK 25 FFM construct |
|---|---|
| 3 syscalls | 3 fixed-arity downcall `MethodHandle`s over variadic `syscall`, `firstVariadicArg(1)`, `captureCallState("errno")`; per-arch `long` nr = 425/426/427. |
| `mmap`/`munmap` | Downcall handles; `munmap` wired as the `reinterpret` cleanup action. |
| `io_uring_params` (120 B) | `structLayout`; read `features`@20, `sq_off`@40, `cq_off`@80 after setup. |
| SQ/CQ offsets (40 B each) | Read once into Java ints; index rings by `base + off`. |
| `io_uring_sqe` (64 B) | Offset-based `JAVA_*_UNALIGNED` VarHandles; `fill(0)` then set opcode/fd/off/addr/len/user_data. |
| `io_uring_cqe` (16 B) | VarHandles at 0/8/12; `res<0` ⇒ `-errno`; decode `flags`. |
| SQ tail publish / CQ head advance | `VarHandle.setRelease`. |
| SQ head read / CQ tail read | `VarHandle.getAcquire`. |
| opcodes | `IORING_OP_READ=22`, `READV=1`, `READ_FIXED=4`, `NOP=0`. |
| setup flags | baseline 0; opt-in `SINGLE_ISSUER|DEFER_TASKRUN` on ≥6.1. |
| registration | `REGISTER_PROBE=8` (opcode probe), `REGISTER_BUFFERS2=15`, `REGISTER_FILES2=13`, `REGISTER_RING_FDS=20`. |
| availability probe | `io_uring_setup(1,&params)` + `NOP`; treat `ENOSYS`/`EPERM` as unavailable → fallback. |

**Verification of the existing plan's ABI claims:** `io_uring_params`=120 B ✓, `io_uring_sqe`=64 B ✓, `io_uring_cqe`=16 B ✓, `sq_off`@40 / `cq_off`@80 ✓, syscall numbers 425/426/427 identical on both arches ✓, `IORING_OP_READ`=22 / `READV`=1 / `READ_FIXED`=4 ✓, mmap offsets ✓. One phrasing correction to plan §3.2: `IORING_FEAT_SINGLE_MMAP` means the SQ **and** CQ rings share **one** mapping, so the total is **2 mmaps (shared ring + SQES)**, not "2 mmaps" as an alternative to a different count — without the feature it is 3. The plan is otherwise ABI-accurate.

Relevant file: `/usr/include/linux/io_uring.h` (authoritative header on this host); verification probe compiled and run at `/tmp/uring_probe.c`.