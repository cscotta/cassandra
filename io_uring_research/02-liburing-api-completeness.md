I have everything I need: the full existing plan, the kernel ABI (`io_uring_sqe`, opcodes, register ops, flags, buf-ring structs) from `/usr/include/linux/io_uring.h`, plus the liburing source-level behavior of each wrapper. Below is the deliverable.

---

# liburing API-Completeness Checklist for a Standalone JDK 25 FFM `io_uring` Wrapper

Scope note: the existing `IO_URING_DISK_ACCESS_MODE_PLAN.md` is **read-path-only** (it names just `IORING_OP_READ`/`READV`/`READ_FIXED` and a hand-rolled SQ/CQ cycle in §3.3). This checklist is the superset for a *general-purpose* wrapper. Its priorities are tuned so the intersection with that plan (setup, get_sqe, prep_read/readv/read_fixed, submit, wait/peek CQE, register_buffers/files) is all **must-have**; socket/filesystem/multishot ops are mostly **nice-to-have** or **omit-for-v1** for Cassandra's storage engine but listed for completeness.

Verified against this tree: opcodes are the 0-indexed `enum io_uring_op` order (NOP=0 … READ=22, WRITE=23 … FTRUNCATE=55); `io_uring_sqe` is 64 B with the union layout at lines 30-118; register ops at line 645; `IORING_OFF_*`, `IORING_SQ_*`, `IORING_ENTER_*`, `IORING_FEAT_*`, `IORING_CQE_*` all confirmed present on this kernel header.

Legend: **[MH]** must-have v1 · **[NH]** nice-to-have · **[X]** omit-for-v1.

---

## Group 1 — Setup / teardown

| liburing symbol | Behavior / syscall & fields | Proposed Java method | Prio |
|---|---|---|---|
| `io_uring_queue_init(entries, ring, flags)` | Zeroes an `io_uring_params`, sets `params.flags=flags`, calls `io_uring_setup(entries,&p)` → ring_fd; then `io_uring_queue_mmap` (2 or 3 `mmap`s at `IORING_OFF_SQ_RING`/`CQ_RING`/`SQES`). Sets up `sq`/`cq` pointer caches from `p.sq_off`/`p.cq_off`. | `IoUring.init(int entries, int flags)` → `IoUring` (AutoCloseable) | **[MH]** |
| `io_uring_queue_init_params(entries, ring, params)` | As above but caller-supplied `io_uring_params` (to request `IORING_SETUP_SQPOLL/CQSIZE/ATTACH_WQ/COOP_TASKRUN/SINGLE_ISSUER/DEFER_TASKRUN`, read back `params.features`). | `IoUring.init(int entries, IoUringParams p)`; expose `features()` | **[MH]** (need `features` for NODROP/SINGLE_MMAP/EXT_ARG) |
| `io_uring_queue_exit(ring)` | `munmap` SQ/CQ/SQE regions, `close(ring_fd)`, `close(enter_ring_fd)` if registered. | `close()` on the confined `Arena`, then `munmap` + `close` syscalls | **[MH]** |
| `io_uring_ring_dontfork(ring)` | `madvise(MADV_DONTFORK)` on the mmap'd regions so a forked child can't corrupt the shared ring. | `dontfork()` | **[NH]** (JVM rarely forks; harmless to add) |
| `io_uring_get_probe(ring)` / `io_uring_get_probe()` | Alloc `io_uring_probe` + op array; `io_uring_register(PROBE)`; used with `io_uring_opcode_supported(probe,op)`. | `probe()` → `IoUringProbe` with `supports(int opcode)` | **[MH]** (opcode feature-gating for READ vs READV fallback) |
| `io_uring_free_probe(probe)` | Frees probe buffer. | Arena-scoped, implicit | **[MH]** (as lifecycle of `probe()`) |

---

## Group 2 — SQE acquisition + the `io_uring_prep_*` family

`io_uring_get_sqe(ring)` — **[MH]**: if `(sqe_tail - sqe_head) < sq_ring_entries` return `&sqes[sqe_tail & sq_ring_mask]` and `sqe_tail++`; else `NULL` (ring full → must submit first). Java: `MemorySegment nextSqe()` returning a 64-B slice, or `-1`/null sentinel.

`io_uring_prep_rw(op, sqe, fd, addr, len, offset)` — **the base [MH]**. Sets `opcode=op`, `flags=0`, `ioprio=0`, `fd=fd`, `off=offset`, `addr=addr`, `len=len`, `rw_flags=0`, `buf_index=0`, `personality=0`, `file_index=0` (`splice_fd_in` union), and zeroes the trailing `addr3`/`__pad2` 16 bytes. Every other prep is a thin wrapper over this — **implement it once in Java as `prepRw(sqe, op, fd, addr, len, off)` that clears all 64 bytes first** (critical: SQEs are reused, see hazards).

| liburing symbol | opcode + sqe fields set | Java method | Prio |
|---|---|---|---|
| `prep_nop` | NOP(0); fd=-1, all zero | `prepNop` | **[NH]** (barrier/testing) |
| `prep_read` | READ(22); fd, addr=buf, len, off | `prepRead` | **[MH]** |
| `prep_write` | WRITE(23); fd, addr=buf, len, off | `prepWrite` | **[MH]** (WAL/flush later) |
| `prep_readv` | READV(1); addr=iovecs, len=nr_vecs, off | `prepReadv` | **[MH]** (pre-5.6 fallback + scatter) |
| `prep_writev` | WRITEV(2); addr=iovecs, len=nr_vecs, off | `prepWritev` | **[NH]** |
| `prep_readv2/prep_writev2` | READV/WRITEV; additionally `rw_flags = RWF_*` (e.g. `RWF_HIPRI`,`RWF_NOWAIT`,`RWF_DSYNC`) | `prepReadv(..., int rwFlags)` overload | **[NH]** (`RWF_NOWAIT`/`HIPRI` useful) |
| `prep_read_fixed` | READ_FIXED(4); addr into registered buf, `buf_index=idx` | `prepReadFixed` | **[MH]** (Phase 2 registered buffers) |
| `prep_write_fixed` | WRITE_FIXED(5); `buf_index` | `prepWriteFixed` | **[NH]** |
| `prep_fsync` | FSYNC(3); fd, `fsync_flags` (0 or `IORING_FSYNC_DATASYNC`) | `prepFsync(fd, flags)` | **[NH]** (fdatasync via ring, later) |
| `prep_poll_add` | POLL_ADD(6); fd, `poll32_events = mask` (LE-swapped on BE) | `prepPollAdd` | **[X]** |
| `prep_poll_multishot` | POLL_ADD(6) + `len = IORING_POLL_ADD_MULTI` | `prepPollMultishot` | **[X]** |
| `prep_poll_remove` | POLL_REMOVE(7); fd=-1, `addr = target user_data` | `prepPollRemove` | **[X]** |
| `prep_poll_update` | POLL_REMOVE(7); `addr=old_ud`, `off=new_ud`, `len=flags`, `poll32_events=mask` | `prepPollUpdate` | **[X]** |
| `prep_timeout` | TIMEOUT(11); fd=-1, `addr=&kernel_timespec`, `len=1`, `off=count`, `timeout_flags` (`ABS`/`BOOTTIME`/`REALTIME`/`ETIME_SUCCESS`/`MULTISHOT`) | `prepTimeout(ts, count, flags)` | **[NH]** (drives `submit_and_wait_timeout` fallback pre-EXT_ARG) |
| `prep_timeout_remove` | TIMEOUT_REMOVE(12); fd=-1, `addr=user_data`, `timeout_flags` | `prepTimeoutRemove` | **[NH]** |
| `prep_timeout_update` | TIMEOUT_REMOVE(12); `addr=user_data`, `off=(uintptr)ts`, `timeout_flags | IORING_TIMEOUT_UPDATE` | `prepTimeoutUpdate` | **[X]** |
| `prep_accept` | ACCEPT(13); fd, `addr=sockaddr`, `off=(u64)&addrlen`, `accept_flags` | `prepAccept` | **[X]** (socket domain) |
| `prep_accept_direct` | ACCEPT(13) + `file_index = idx+1` (or `IORING_FILE_INDEX_ALLOC`) | `prepAcceptDirect` | **[X]** |
| `prep_multishot_accept(_direct)` | ACCEPT(13) + `ioprio |= IORING_ACCEPT_MULTISHOT` (+ file_index=ALLOC for direct) | `prepMultishotAccept[Direct]` | **[X]** |
| `prep_connect` | CONNECT(16); fd, `addr=sockaddr`, `off=addrlen` | `prepConnect` | **[X]** |
| `prep_send` | SEND(26); fd, `addr=buf`, `len`, `msg_flags` | `prepSend` | **[X]** |
| `prep_send_zc` | SEND_ZC(47); like send + `ioprio=zc_flags`; emits 2 CQEs (`F_MORE` then `F_NOTIF`) | `prepSendZc` | **[X]** |
| `prep_sendto` | SEND(26) + `send_set_addr`: `addr2=dest`, `addr_len` | `prepSendto` | **[X]** |
| `prep_recv` | RECV(27); fd, `addr=buf`, `len`, `msg_flags` | `prepRecv` | **[X]** |
| `prep_recv_multishot` | RECV(27) + `ioprio |= IORING_RECV_MULTISHOT` | `prepRecvMultishot` | **[X]** |
| `prep_sendmsg` | SENDMSG(9); fd, `addr=&msghdr`, `len=1`, `msg_flags` | `prepSendmsg` | **[X]** |
| `prep_recvmsg` | RECVMSG(10); `addr=&msghdr`, `len=1`, `msg_flags` | `prepRecvmsg` | **[X]** |
| `prep_recvmsg_multishot` | RECVMSG(10) + `ioprio |= IORING_RECV_MULTISHOT` | `prepRecvmsgMultishot` | **[X]** |
| `prep_sendmsg_zc` | SENDMSG_ZC(48) | `prepSendmsgZc` | **[X]** |
| `prep_openat` | OPENAT(18); fd=dfd, `addr=path`, `len=mode`, `open_flags` | `prepOpenat` | **[NH]** (async SSTable open) |
| `prep_openat2` | OPENAT2(28); fd=dfd, `addr=path`, `len=sizeof(open_how)`, `off=(u64)&open_how` | `prepOpenat2` | **[NH]** |
| `prep_openat_direct/openat2_direct` | as above + `file_index` (slot or ALLOC) | `prepOpenat[2]Direct` | **[NH]** (registered-file open) |
| `prep_close` | CLOSE(19); fd | `prepClose` | **[NH]** |
| `prep_close_direct` | CLOSE(19); fd=0, `file_index=idx+1` | `prepCloseDirect` | **[NH]** |
| `prep_statx` | STATX(21); fd=dfd, `addr=path`, `len=mask`, `off=(u64)&statxbuf`, `statx_flags` | `prepStatx` | **[X]** |
| `prep_fallocate` | FALLOCATE(17); fd, `len=mode`, `off=offset`, `addr=(u64)len` | `prepFallocate` | **[NH]** (preallocate SSTable) |
| `prep_ftruncate` | FTRUNCATE(55); fd, `off=len` | `prepFtruncate` | **[NH]** |
| `prep_fadvise` / `prep_fadvise64` | FADVISE(24); fd, `off=offset`, `len=len` (64b variant `addr=len`), `fadvise_advice` | `prepFadvise` | **[NH]** (`POSIX_FADV_DONTNEED`/`WILLNEED`) |
| `prep_madvise` / `prep_madvise64` | MADVISE(25); fd=-1, `addr`, `len`, `fadvise_advice` | `prepMadvise` | **[X]** |
| `prep_splice` | SPLICE(30); fd_out, `splice_off_in=off_in`, `off=off_out`, `len=nbytes`, `splice_fd_in=fd_in`, `splice_flags` | `prepSplice` | **[X]** (streaming/zero-copy later) |
| `prep_tee` | TEE(33); fd_out, `len=nbytes`, `splice_fd_in=fd_in`, `splice_flags` | `prepTee` | **[X]** |
| `prep_provide_buffers` | PROVIDE_BUFFERS(31); fd=nr, `addr=base`, `len=buf_size`, `off=start_bid`, `buf_group=bgid` | `prepProvideBuffers` | **[X]** (classic; prefer buf-ring) |
| `prep_remove_buffers` | REMOVE_BUFFERS(32); fd=nr, `buf_group=bgid` | `prepRemoveBuffers` | **[X]** |
| `prep_cancel` / `prep_cancel64` | ASYNC_CANCEL(14); fd=-1, `addr=user_data`, `cancel_flags` (`ALL`/`FD`/`ANY`/`FD_FIXED`/`USERDATA`/`OP`) | `prepCancel(long ud, int flags)` | **[NH]** (cancel on timeout/shutdown) |
| `prep_link_timeout` | LINK_TIMEOUT(15); fd=-1, `addr=&ts`, `len=1`, `timeout_flags`; must be `IOSQE_IO_LINK`ed to prior SQE | `prepLinkTimeout` | **[NH]** (per-I/O deadline) |
| `prep_files_update` | FILES_UPDATE(20); fd=-1, `addr=&fds[]`, `len=nr`, `off=offset` | `prepFilesUpdate` | **[NH]** |
| `prep_shutdown` | SHUTDOWN(34); fd, `len=how` | `prepShutdown` | **[X]** |
| `prep_socket(_direct)` | SOCKET(45); fd=domain, `off=type`, `len=protocol`, `rw_flags=flags` (+file_index for direct) | `prepSocket[Direct]` | **[X]** |
| `prep_renameat` | RENAMEAT(35); fd=olddfd, `addr=oldpath`, `len=newdfd`, `off=(u64)newpath`, `rename_flags` | `prepRenameat` | **[NH]** (atomic SSTable rename) |
| `prep_unlinkat` | UNLINKAT(36); fd=dfd, `addr=path`, `unlink_flags` | `prepUnlinkat` | **[NH]** |
| `prep_mkdirat` | MKDIRAT(37); fd=dfd, `addr=path`, `len=mode` | `prepMkdirat` | **[X]** |
| `prep_symlinkat` | SYMLINKAT(38); fd=newdfd, `addr=target`, `addr2=linkpath` | `prepSymlinkat` | **[X]** |
| `prep_linkat` | LINKAT(39); fd=olddfd, `addr=oldpath`, `len=newdfd`, `off=(u64)newpath`, `hardlink_flags` | `prepLinkat` | **[X]** |
| `prep_msg_ring` | MSG_RING(40); fd=target_ring_fd, `len=arbitrary`, `off=data`, `msg_ring_flags` (`IORING_MSG_DATA`) | `prepMsgRing` | **[NH]** (cross-ring wakeup between poller/carrier rings) |
| `prep_msg_ring_cqe_flags` / `prep_msg_ring_fd` | MSG_RING variants: pass cqe flags / send a fixed fd (`IORING_MSG_SEND_FD`) | `prepMsgRingCqeFlags` / `prepMsgRingFd` | **[X]** |
| `prep_fixed_fd_install` | FIXED_FD_INSTALL(54); fd=fixed_slot, `flags=IOSQE_FIXED_FILE`, `install_fd_flags` → returns a real fd in CQE res | `prepFixedFdInstall` | **[X]** |

Also **[MH]** helper wrappers used by many preps: `io_uring_prep_rw` (base, above) and the `__io_uring_set_target_fixed_file` logic (`file_index = idx==ALLOC ? ALLOC : idx+1`) for all `*_direct` variants.

---

## Group 3 — Submission / completion

| liburing symbol | Behavior | Java method | Prio |
|---|---|---|---|
| `io_uring_submit(ring)` | `__io_uring_flush_sq()` → `submitted`; if `sq_ring_needs_enter(submitted,&flags)` call `io_uring_enter(fd, submitted, 0, flags, NULL)`. Returns # SQEs consumed by kernel. | `submit()` → int | **[MH]** |
| `io_uring_submit_and_wait(ring, wait_nr)` | flush; enter with `min_complete=wait_nr` and `IORING_ENTER_GETEVENTS`. | `submitAndWait(int)` | **[MH]** |
| `io_uring_submit_and_wait_timeout(ring, &cqe, wait_nr, ts, sigmask)` | If `IORING_FEAT_EXT_ARG`: single enter with `IORING_ENTER_EXT_ARG` + `io_uring_getevents_arg{ts,sigmask}`. Else fallback: `prep_timeout` SQE + submit_and_wait. Returns and fills first cqe ptr. | `submitAndWaitTimeout(int, KernelTimespec)` | **[MH]** (bounded poller wait) |
| `io_uring_wait_cqe(ring, &cqe)` | `_io_uring_get_cqe(submit=0, wait_nr=1)`; blocks in `enter GETEVENTS` until ≥1 CQE. | `waitCqe()` → cqe segment | **[MH]** |
| `io_uring_wait_cqe_nr(ring, &cqe, nr)` | get_cqe with `wait_nr=nr`. | `waitCqeNr(int)` | **[NH]** |
| `io_uring_wait_cqe_timeout(ring, &cqe, ts)` | EXT_ARG path or timeout-SQE fallback; wait ≥1 CQE or timeout (`-ETIME`). | `waitCqeTimeout(ts)` | **[NH]** |
| `io_uring_peek_cqe(ring, &cqe)` | `__io_uring_peek_cqe`: read `cq.ktail` (acquire) vs `cq.khead`; return head CQE without waiting (`-EAGAIN`/NULL if none). Also flags overflow-flush need. | `peekCqe()` → cqe or null | **[MH]** |
| `io_uring_peek_batch_cqe(ring, cqes[], count)` | Drain up to `count` ready CQEs into an array (respecting `CQE32` stride); may trigger overflow flush first. | `peekBatch(long[] outUserData/int[] outRes, int max)` | **[MH]** (batch reap = fewer VarHandle round-trips) |
| `io_uring_cqe_seen(ring, cqe)` | `cq_advance(ring,1)`. | `cqeSeen()` | **[MH]** |
| `io_uring_cq_advance(ring, nr)` | `*cq.khead = khead + nr` (release store). Batch-ack N CQEs. | `cqAdvance(int)` | **[MH]** |
| `io_uring_for_each_cqe(ring, head, cqe)` | Macro: iterate `head = *khead` while `head != atomic_load_acquire(ktail)`, `cqe = &cqes[head & mask]` (stride `1<<CQE32`). | idiom in `peekBatch` / a `forEachCqe(consumer)` | **[MH]** |
| `io_uring_sq_ready(ring)` | `sqe_tail - atomic_load_acquire(*sq.khead)` → SQEs prepped but not yet consumed by kernel. | `sqReady()` | **[NH]** |
| `io_uring_sq_space_left(ring)` | `sq_ring_entries - sq_ready()`. | `sqSpaceLeft()` | **[MH]** (backpressure guard before `get_sqe`) |
| `io_uring_cq_ready(ring)` | `atomic_load_acquire(*cq.ktail) - *cq.khead`. | `cqReady()` | **[MH]** |
| `io_uring_sqring_wait(ring)` | SQPOLL only: `enter(0,0,IORING_ENTER_SQ_WAIT)` to wait for SQ slots when full. | `sqringWait()` | **[X]** (only if SQPOLL opt-in used) |

---

## Group 4 — Internal barrier / index management (reimplement exactly)

These are not public entry points but their logic **must** be reproduced correctly in Java. The plan's §3.3 sketch is under-specified — this is the precise contract.

**Cached indices (per ring, plain Java longs, NOT shared with kernel):** `sqe_head`, `sqe_tail`. **Shared with kernel (mmap'd, need acquire/release):** `*sq.khead`, `*sq.ktail`, `sq.array[]`, `*sq.kflags`, `*cq.khead`, `*cq.ktail`.

- **`io_uring_get_sqe`** — **[MH]**: `next = sqe_tail + 1`; if `next - sqe_head <= sq_ring_entries` (space): `sqe = &sqes[sqe_tail & sq_ring_mask]`; `sqe_tail = next`; return sqe. Else return NULL. (No kernel interaction; purely local cursor.)

- **`__io_uring_flush_sq`** — **[MH]**: `to_submit = sqe_tail - sqe_head`. For the default (has `sq.array`) case, for each pending SQE set `sq.array[(*ktail_local) & mask] = (sqe_head & mask)` while walking `sqe_head` up to `sqe_tail`, incrementing a local tail. Then **release-store** `*sq.ktail = old_ktail + to_submit` (this is the single publication point — kernel must not see the array writes reorder after the tail bump, hence `setRelease`). Return `to_submit`. With `IORING_SETUP_NO_SQARRAY` the array step is skipped. **Java: use `VarHandle.setRelease` for `*ktail`; plain writes for `array[]` (ordered before the release).**

- **`io_uring_sq_ring_needs_enter(submit, &flags)`** — **[MH]**:
  - If `!(setup_flags & IORING_SETUP_SQPOLL)` → **return true** (non-SQPOLL always enters when there is work).
  - Else (SQPOLL): issue a full memory barrier (acquire), then read `*sq.kflags`; if `IORING_SQ_NEED_WAKEUP` is set → set `*flags |= IORING_ENTER_SQ_WAKEUP` and **return true** (kernel poll thread is asleep, must wake it). Otherwise **return false** (skip the syscall entirely — the whole point of SQPOLL).

- **`io_uring_cq_ring_needs_flush(ring)`** — **[MH]**: `atomic_load(*sq.kflags) & (IORING_SQ_CQ_OVERFLOW | IORING_SQ_TASKRUN)` — CQ overflowed into the kernel backlog, or deferred taskrun pending; either needs an `enter(GETEVENTS)` to drain.

- **`io_uring_cq_ring_needs_enter(ring)`** — **[MH]**: `(setup_flags & IORING_SETUP_IOPOLL) || cq_ring_needs_flush(ring)`. IOPOLL always needs a kernel entry to reap; overflow needs a flush.

- **`io_uring_submit` composition** — **[MH]**: `submitted = flush_sq()`; compute `needs_enter = sq_ring_needs_enter(submitted,&flags) || (wait_nr && cq_ring_needs_enter())`; if `needs_enter` → `enter(fd, submitted, wait_nr, flags|GETEVENTS?, arg)`; else return `submitted` without syscall. **Return value = number the kernel consumed** (may be < submitted only in rare `SUBMIT_ALL`/error partials; normally == submitted).

Java realization: keep `sqeHead`/`sqeTail` as fields; wrap `khead/ktail/kflags` with `VarHandle` (`OfLong`) over the mmap `MemorySegment`; `getAcquire` for reads-from-kernel, `setRelease` for writes-to-kernel. A `loadFence()`/`VarHandle.acquireFence()` is required before the SQPOLL `kflags` read.

---

## Group 5 — Registration wrappers (`io_uring_register`)

| liburing symbol | register opcode + arg | Java method | Prio |
|---|---|---|---|
| `register_buffers(iovecs, nr)` | `REGISTER_BUFFERS(0)`, arg=`iovec[]` | `registerBuffers(iovecs)` | **[MH]** (Phase 2, `READ_FIXED`) |
| `register_buffers_sparse(nr)` | `REGISTER_BUFFERS2(15)` with `rsrc_register{nr,flags=SPARSE}` | `registerBuffersSparse(nr)` | **[NH]** |
| `register_buffers_tags(iovecs,tags,nr)` | `REGISTER_BUFFERS2(15)` + tag array | `registerBuffersTags` | **[NH]** |
| `register_buffers_update_tag(off,iovecs,tags,nr)` | `REGISTER_BUFFERS_UPDATE(16)` `rsrc_update2` | `registerBuffersUpdate` | **[NH]** |
| `unregister_buffers` | `UNREGISTER_BUFFERS(1)` | `unregisterBuffers` | **[MH]** |
| `register_files(fds, nr)` | `REGISTER_FILES(2)`, arg=`int[] fds` | `registerFiles(int[])` | **[MH]** (fixed SSTable fds) |
| `register_files_sparse(nr)` | `REGISTER_FILES2(13)` `{nr,flags=SPARSE}` (kernel ≥5.19) | `registerFilesSparse(nr)` | **[MH]** (alloc slots for openat_direct) |
| `register_files_tags/_update_tag` | `REGISTER_FILES2(13)`/`FILES_UPDATE2(14)` + tags | `registerFilesTags/Update` | **[NH]** |
| `register_files_update(off, fds, nr)` | `REGISTER_FILES_UPDATE(6)` `rsrc_update{off,fds}` | `registerFilesUpdate` | **[MH]** (rotate fds without full re-register) |
| `unregister_files` | `UNREGISTER_FILES(3)` | `unregisterFiles` | **[MH]** |
| `register_eventfd(fd)` | `REGISTER_EVENTFD(4)`, arg=`&int` | `registerEventfd` | **[NH]** (integrate CQE readiness with an epoll/selector) |
| `register_eventfd_async(fd)` | `REGISTER_EVENTFD_ASYNC(7)` (only async completions signal) | `registerEventfdAsync` | **[NH]** |
| `unregister_eventfd` | `UNREGISTER_EVENTFD(5)` | `unregisterEventfd` | **[NH]** |
| `register_probe(probe, nr)` | `REGISTER_PROBE(8)` | (see Group 1 `probe()`) | **[MH]** |
| `register_personality` | `REGISTER_PERSONALITY(9)` → returns id | `registerPersonality` | **[X]** |
| `unregister_personality(id)` | `UNREGISTER_PERSONALITY(10)` | `unregisterPersonality` | **[X]** |
| `register_restrictions(res, nr)` | `REGISTER_RESTRICTIONS(11)` (only before `enable_rings`) | `registerRestrictions` | **[X]** (sandboxing) |
| `enable_rings(ring)` | `ENABLE_RINGS(12)` — start a ring created with `IORING_SETUP_R_DISABLED` | `enableRings` | **[X]** |
| `setup_buf_ring(ring, nentries, bgid, flags, &ret)` | Alloc ring (`mmap`/aligned), `register_buf_ring`, `buf_ring_init`. | `setupBufRing(...)` → `IoUringBufRing` | **[NH]** (provided-buffer recv; storage read path doesn't need it) |
| `register_buf_ring(ring, &reg, flags)` | `REGISTER_PBUF_RING(22)`, arg=`io_uring_buf_reg{ring_addr,ring_entries,bgid}` | `registerBufRing` | **[NH]** |
| `free_buf_ring/unregister` | `UNREGISTER_PBUF_RING(23)` + munmap | `freeBufRing` | **[NH]** |
| `io_uring_buf_ring_init(br)` | `br->tail = 0` | `BufRing.init()` | **[NH]** |
| `io_uring_buf_ring_add(br, addr, len, bid, mask, off)` | `buf=&br->bufs[(tail+off)&mask]`; set `addr,len,bid` | `BufRing.add(...)` | **[NH]** |
| `io_uring_buf_ring_advance(br, n)` | release-store `br->tail += n` | `BufRing.advance(n)` | **[NH]** |
| `io_uring_buf_ring_cq_advance(ring, br, n)` | advance both CQ head and buf-ring tail together | `BufRing.cqAdvance(n)` | **[NH]** |
| `register_ring_fd(ring)` | `REGISTER_RING_FDS(20)` → sets `enter_ring_fd`, enables `IORING_ENTER_REGISTERED_RING` (saves an fdget per enter) | `registerRingFd()` | **[NH]** (perf; needs `IORING_FEAT_REG_REG_RING`) |
| `unregister_ring_fd(ring)` | `UNREGISTER_RING_FDS(21)` | `unregisterRingFd()` | **[NH]** |
| `register_sync_cancel(ring, &reg)` | `REGISTER_SYNC_CANCEL(24)`, arg=`io_uring_sync_cancel_reg{addr,fd,flags,timeout}` — synchronous cancel | `registerSyncCancel(...)` | **[NH]** (clean shutdown drain) |
| `register_file_alloc_range(ring, off, len)` | `REGISTER_FILE_ALLOC_RANGE(25)`, arg=`io_uring_file_index_range` | `registerFileAllocRange` | **[X]** |

---

## Group 6 — Data / flag helpers

| liburing symbol | Behavior | Java method | Prio |
|---|---|---|---|
| `io_uring_sqe_set_data(sqe, ptr)` | `sqe->user_data = (u64) ptr` | `sqeSetData(sqe, long)` — Java has no raw ptr; **use a 64-bit correlation token** into a `LongObjectHashMap<PendingIo>` | **[MH]** |
| `io_uring_sqe_set_data64(sqe, u64)` | `sqe->user_data = data` | `sqeSetData64(sqe, long)` | **[MH]** |
| `io_uring_cqe_get_data(cqe)` | `(void*) cqe->user_data` | (same as get_data64) | **[MH]** |
| `io_uring_cqe_get_data64(cqe)` | `cqe->user_data` | `cqeGetData64(cqe)` → long | **[MH]** |
| `io_uring_sqe_set_flags(sqe, flags)` | `sqe->flags = flags` (`IOSQE_FIXED_FILE`/`IO_LINK`/`IO_HARDLINK`/`IO_DRAIN`/`ASYNC`/`BUFFER_SELECT`/`CQE_SKIP_SUCCESS`) | `sqeSetFlags(sqe, int)` | **[MH]** (FIXED_FILE + IO_LINK are the storage-relevant ones) |
| `io_uring_cqe_get_data` buffer-id decode | `bid = cqe->flags >> IORING_CQE_BUFFER_SHIFT` (16); valid only if `flags & IORING_CQE_F_BUFFER` | `cqeBufferId(int flags)` | **[NH]** (only with provided buffers) |
| (`cqe->flags & IORING_CQE_F_MORE`) | more CQEs to come for a multishot SQE | `cqeHasMore(flags)` | **[NH]** |

Java note: `user_data` is the *only* correlation channel back to Java state. Since FFM gives you no stable object pointers, allocate a monotonic `long` token per in-flight op and map token→`PendingIo` (buffer segment, `CompletableFuture`, fd, len). This replaces liburing's "stuff a pointer in user_data" idiom.

---

## Group 7 — Subtle behaviors to replicate carefully (hazards)

1. **SQE reuse / caching.** `get_sqe` hands back a slot in the fixed `sqes[]` array that was used by a *previous, already-completed* op. Its 64 bytes still hold stale data. `prep_rw` **fully overwrites** every field precisely so stale bytes can't leak (e.g. a stale `buf_index`, `rw_flags`, `personality`, or the `addr3` tail). **In Java, `prepRw` must zero the full 64-B slice (or explicitly set every union field) before setting op-specific fields.** This is the single most common correctness bug in hand-rolled rings.

2. **Over-submission / SQ full.** `get_sqe` returns NULL when `sqe_tail - sqe_head == sq_ring_entries`. You must `submit()` (or wait) to let the kernel consume before prepping more. Never bump `sqe_tail` past capacity.

3. **CQ overflow.** If completions arrive faster than reaped and CQ is full: with `IORING_FEAT_NODROP` (kernel ≥5.5, check `features`) the kernel keeps an overflow backlog and sets `IORING_SQ_CQ_OVERFLOW` in `*sq.kflags` — you must call `enter(...GETEVENTS)` (even with `to_submit=0`) to flush the backlog into the CQ; `cq_ring_needs_flush()` detects this. **Without NODROP, overflow CQEs are dropped** and `cq_overflow` counter increments — then you must size CQ (`IORING_SETUP_CQSIZE`) generously and reap promptly. Replicate: check `kflags` on every reap loop; if overflow bit set, force a flushing enter.

4. **needs_enter under SQPOLL wakeup.** With SQPOLL the kernel poll thread consumes SQEs without any syscall — but it parks after `sq_thread_idle` ms and sets `IORING_SQ_NEED_WAKEUP`. Submitting while parked requires `enter(..., IORING_ENTER_SQ_WAKEUP)`. Skipping this = SQEs sit forever. The barrier before reading `kflags` matters (Group 4). Non-SQPOLL always enters, so this only bites if you enable the `io_uring_sqpoll` opt-in.

5. **`submit()` return-value semantics.** Returns how many SQEs the kernel accepted this call, **not** how many completed. Normally equals what you flushed. It can be less with `IORING_SETUP_SUBMIT_ALL` semantics or an early error on a linked chain; without `SUBMIT_ALL`, a bad SQE aborts the rest of the batch. Track submitted vs. prepped to avoid double-submitting the same SQEs.

6. **EINTR / EAGAIN / EBUSY / ETIME retry.**
   - `enter` returning `-EINTR` (signal) → retry the enter (idempotent; it hasn't consumed extra).
   - `-EAGAIN` on submit → resources momentarily unavailable; back off/retry.
   - `-EBUSY` → CQ overflow backlog not yet flushed / too many in-flight; must reap CQEs (flushing enter) before submitting more. The plan's backpressure `Semaphore(queue_depth)` should gate this proactively.
   - `-ETIME` from a timeout wait is *success* (timer fired), not an error.
   - A per-op failure comes back as `cqe->res < 0` = `-errno` (e.g. `-EIO`, `-EBADF`), **not** as a syscall error — decode per-CQE and complete that op's future exceptionally, don't fail the whole ring.

7. **Short reads.** `cqe->res` is bytes transferred and may be `< len` (esp. at EOF or with O_DIRECT). `res == 0` = EOF. The caller must loop/re-submit for the remainder — the ring does not auto-complete partial reads (this is already flagged in the plan's alignment section but belongs in the binding contract).

8. **Memory ordering (Group 4).** `ktail` publish = `setRelease`; `khead`/`ktail` reads that gate on kernel progress = `getAcquire`; SQPOLL `kflags` read needs an `acquireFence` first. Using plain loads/stores here produces rare, unreproducible lost-wakeups and phantom-empty CQ bugs. Given JDK 25 FFM `MemorySegment` + `VarHandle`, use the acquire/release `VarHandle` modes, not `get()/set()`.

9. **O_DIRECT triple alignment** (addr, offset, length to 512/4096) — already covered in plan §3.4; belongs on this list because a misaligned `READ_FIXED` returns `-EINVAL` in the CQE, not at submit.

---

## Minimum viable general-purpose API (v1)

Everything Cassandra's read path (and a clean generic core) needs, nothing more:

1. **Lifecycle:** `init(entries, params)`, `close()`, `features()`, `probe()/supports(op)`.
2. **SQE:** `nextSqe()` (get_sqe), `prepRw()` (base), `prepRead`, `prepReadv`, `prepReadFixed`, `prepWrite`, `prepFsync`; `sqeSetData64`, `sqeSetFlags`.
3. **Submit/complete:** `submit()`, `submitAndWait(n)`, `submitAndWaitTimeout(n,ts)`, `waitCqe()`, `peekCqe()`, `peekBatch()`, `cqAdvance(n)`/`cqeSeen()`, `cqeGetData64()`, `cqeRes()`.
4. **Ring accounting:** `sqSpaceLeft()`, `sqReady()`, `cqReady()`, plus the exact internal `flushSq`/`sqRingNeedsEnter`/`cqRingNeedsFlush` logic.
5. **Registration (Phase 2):** `registerFiles`/`registerFilesSparse`/`registerFilesUpdate`/`unregisterFiles`; `registerBuffers`/`unregisterBuffers`.
6. **Correlation infra:** monotonic `long` user_data token ↔ `LongObjectHashMap<PendingIo>`.

That is ~6 prep methods + ~10 submit/complete methods + registration, versus the ~120 symbols in full liburing. The plan's §3.3 already implies items 1-4 and §3.4 implies item 5.

## Hazards when reimplementing (quick reference)

- **Zero the whole 64-B SQE** in `prepRw` — stale-field leakage is the #1 bug.
- **Acquire/release `VarHandle` modes** on all kernel-shared indices; `acquireFence` before SQPOLL `kflags`.
- **Reap before you exhaust the SQ**; honor `-EBUSY`/CQ-overflow by forcing a `GETEVENTS` flush.
- **Per-CQE errors are `-errno` in `res`**, not syscall failures; short reads and `res==0` EOF are normal, not errors.
- **`submit()` returns consumed count, not completions**; don't re-submit the same SQEs.
- **`-EINTR`/`-EAGAIN` retry, `-ETIME` is success.**
- **CI compiles on JDK 17** (per your JDK25 notes): all FFM/`Linker`/`MemorySegment` code must sit behind a Linux-and-JDK25 runtime guard, and `--enable-native-access=ALL-UNNAMED` must be added to `conf/jvm25-server.options` (confirmed absent; only `--add-opens ...sun.nio.ch` is present).
- **Buffer lifetime:** the DMA buffer segment must outlive the SQE *and* the CQE (kernel writes into it asynchronously); never free/reuse until the completion is reaped — mirror this with the `PendingIo` token holding the segment reference.
- **SQPOLL is opt-in only** (burns a core, needs privileges pre-5.12); the default non-SQPOLL path is simpler and always calls `enter`.

Reference file confirming the ABI used above: `/usr/include/linux/io_uring.h` on this host (kernel 7.0.13, full opcode set through `IORING_OP_PIPE`/`FTRUNCATE`). Existing plan: `/home/cscotta/projects/cassandra/IO_URING_DISK_ACCESS_MODE_PLAN.md`.