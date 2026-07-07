I have gathered sufficient primary-source material across all five areas. Compiling the hardening research now as my return value.

---

# io_uring Java-binding prior-art & hazard research (hardening input for `IO_URING_DISK_ACCESS_MODE_PLAN.md`)

Scope note: this refines/verifies the plan rather than restating it. Where the plan is imprecise I flag it inline as **[plan correction]**.

## 1. Prior-art comparison — existing Java io_uring bindings

| Binding | Binding tech | Scope | API shape | Notable design choices | Maintenance (as of mid-2026) | License |
|---|---|---|---|---|---|---|
| **Netty io_uring** (`netty-incubator-transport-io_uring`, now folded into Netty 4.2 `transport-native-io_uring`) | **JNI** over a bundled C layer (liburing-style), per-arch `.so` shipped in the jar | **Sockets only** — TCP/UDP/domain sockets as a Netty `EventLoopGroup`/`Channel` transport. **No file/pread API at all.** | Netty `ChannelFuture` / event-loop callbacks | Ring per `EventLoop`; integrates into Netty's existing reactor; buffers are Netty `ByteBuf`. Incubator repo **archived Apr 3 2025**, merged into Netty 4.2 (GA Mar 2025). | Active (inside Netty 4.2) | Apache-2.0 |
| **nio_uring** (`bbeaupain/nio_uring`, `sh.blake.niouring`) | **JNI** linking system **liburing** (`LIBURING_PATH` at build time) | Sockets **and** files (`IoUringFile`, `IoUringServerSocket`) | **Callback** (`onRead`/`onWrite`/`onException`) + `loop()`/`execute()` | Zero-copy via direct `ByteBuffer` addressed by kernel; single knob `ringSize` (default 512); **explicitly not thread-safe** — all ops must be issued from the ring's own handler; "one buffer per op per ring execution" constraint; all exceptions unchecked. | Last push Aug 2024; ~182★; effectively dormant | MIT |
| **jasyncfio** (`ikorennoy/jasyncfio`, `one.jasyncfio`) | **JNI**, bundled native lib, **linux-amd64 only** (arm64 "planned", never shipped) | **File I/O focused** (buffered + O_DIRECT); some socket ops | **`CompletableFuture`** (`AsyncFile.read → CompletableFuture<Integer>`) | Dedicated `EventExecutor`; uses registered buffers/files, polling; requires **kernel ≥ 5.11**; clean future-based API is the closest ergonomic match to what the plan wants. | Last commit Apr 2023; ~83★; abandoned; v0.0.8 only, amd64-only | Apache-2.0 |
| **JUring** (`davidtos/JUring`) | **Pure Panama FFM downcalls into system liburing** (`LibUringDispatcher` uses `Linker.nativeLinker()`+`SymbolLookup`; `LibCDispatcher` for malloc/free). **No JNI, but still requires `liburing.so` at runtime.** | **File I/O** (read/write/readv, registered files, open/close), some socket ops | Both **blocking** (`JUringBlocking`, aimed at virtual threads) and **completion-drain** styles | Registered files (+489% at 4 KiB in its JMH); explicit GC-avoidance types (`ZeroGcCqe`, `UserDataPool`, `IovecBlockPool`); QD 256; JMH matrices across buffer size × thread count × registered/unregistered; shows FileChannel *wins* at ≥20 writer threads / 64 KiB — a useful cautionary data point. | Active, last push May 2026; ~197★ | Unlicense (public-domain-equivalent; ASF-acceptable) |
| **PanamaUring** (`dreamlike-ocean/PanamaUring`) | **Pure Panama FFM, reimplements liburing in Java (raw syscalls) — NO native artifact, no liburing dependency** | **Unified file + socket** (async read/write/fsync, connect/accept/recv incl. multishot, splice/sendfile, inotify, eventfd, pipe, poll, madvise, full epoll binding) | **`CancelableFuture`** + EventLoop; Kotlin-coroutine bindings | The most feature-complete pure-FFM prior art. **Ownership/"rent" model borrowed from monoio**: `asyncRead` *takes* the buffer's ownership and *returns* it via `BufferResult` on completion; **memory-safe cancel** (buffer only returned after the future completes, ECANCELED handled); eventfd-based wakeup (copies JDK Selector trick, avoids the `IORING_OP_NOP` race); SQE fill confined to the owning EventLoop. Requires **JDK 24/25** (uses ClassFile + declarative FFI generator), kernel ≥ 5.10. | Active, last push Nov 2025; ~145★; docs primarily Chinese; "exploratory" | MIT |
| **cheshire** (`armanbilge/cheshire`), `sherman/io_uring_panama`, `tkowalcz/io_uring_4j`, `ChinaXing/io_uring-java` | Panama FFM / JNI | Experimental | — | Toy/PoC scope, effectively inactive | ★0–7 | mixed (Apache-2.0 / none) |

**What a fully-featured, extractable, pure-FFM library adds over each:**
- **vs Netty** — Netty's io_uring is socket-only with no disk-read surface; unusable for the SSTable read path. It also drags in the whole Netty transport/reactor model.
- **vs nio_uring** — removes the JNI + build-time `liburing` linkage, the callback-only API, the "not thread-safe / one-buffer-per-op" foot-guns, and the unchecked-exception-only contract; adds arm64.
- **vs jasyncfio** — jasyncfio has the right (`CompletableFuture`) shape but is **amd64-only, JNI, abandoned since 2023**. A pure-FFM binding gets aarch64 for free (Cassandra targets both), no native artifact to package per-arch, and a maintained baseline on JDK 25.
- **vs JUring** — JUring is FFM but **still needs `liburing.so` installed** (an ops/packaging dependency and a version-skew surface); Unlicense; no availability-probe/fallback story; its own data shows regressions at high writer concurrency — informs Gate 0.
- **vs PanamaUring** — the strongest reference implementation and the one to *study* (rent model, memory-safe cancel, eventfd wakeup), but it is exploratory, Chinese-documented, socket-heavy, coroutine-oriented, and far larger in surface than Cassandra needs. Cassandra wants a **minimal file-read opcode set** (`READ`/`READV`/`READ_FIXED`+`FSYNC`), raw-syscall (no liburing), Apache-licensed, cross-arch-tested, with a first-class probe+fallback contract.

**Net:** the plan's Option A (raw-`syscall` FFM, no native artifact) is *unique* among Java bindings except PanamaUring — and even PanamaUring isn't extractable/ASF-shippable as-is. The library's differentiator is **"pure-FFM, zero native artifact, file-read-only, safe probe+fallback, memory-safety enforced at runtime."**

---

## 2. Operational / security posture the plan MUST handle

io_uring is **the single most-restricted-in-practice** Linux subsystem right now. The probe/fallback path is not optional polish — for a large fraction of Cassandra deployments io_uring will be *unavailable by policy*, and the code must degrade silently to `standard`.

**Environments that disable or block io_uring:**

| Environment | Behavior | What the JVM sees |
|---|---|---|
| **Docker / containerd default seccomp** | `io_uring_setup/enter/register` are **not on the allowlist**; profile `defaultAction = SCMP_ACT_ERRNO`. Deliberately excluded for container-escape risk (moby/moby **#46762**). | `io_uring_setup` → **EPERM**. This is the common case for containerized Cassandra. |
| **kernel.io_uring_disabled sysctl** (added **Linux 6.6**) | `0` = allowed (default); `1` = only `CAP_SYS_ADMIN` or members of `io_uring_group` (gid, default `-1` = nobody) may create rings; `2` = disabled for **all** processes. Existing rings keep working. | `1`/`2` → `io_uring_setup` returns **EPERM**. Distros/hardened images increasingly ship `1` or `2`. |
| **gVisor / GKE Sandbox** | Partial, **disabled-by-default**, "basic I/O only" implementation; does not fully support io_uring syscalls; expects runtimes to probe and fall back. | Setup may fail (**ENOSYS/EPERM**) or ops may fail even if setup succeeds — hence probe must *exercise a real op*, not just setup. |
| **ChromeOS** | io_uring **disabled entirely** (Google, 2023). | ENOSYS/EPERM. |
| **Android** | io_uring **disabled for apps** on production kernels (Google, 2023). | ENOSYS/EPERM. |
| **Google production servers** | io_uring **disabled** except where specifically needed (Google, 2023). | — |
| **systemd hardening** | Units with `SystemCallFilter=` (e.g. `~@io_uring`, or an allowlist) filter the io_uring syscall group. Default action can be `SCMP_ACT_ERRNO` **or `SCMP_ACT_KILL`/SIGSYS**. | EPERM **or the process is killed** — see the critical probe hazard below. |
| **Cloud/managed** | Many managed-Linux and hardened base images (e.g. RHEL hardening guides, several CSP hardened images) set `io_uring_disabled=2`. | EPERM. |

**Why it's restricted (CVE history summary):** In June 2023 Google reported that **~60% of Linux-kernel exploit submissions to its kCTF VRP in 2022 exploited io_uring**, and responded by disabling it on Android (apps), ChromeOS (entirely), and Google production servers. The bug classes are the dangerous ones for a memory-unsafe async subsystem: use-after-free and type-confusion in the async work path, and out-of-bounds via registered/fixed buffers (representative examples include the registered-buffer OOB class fixed in 2023 and several io_uring UAFs weaponized in kCTF). Docker then removed io_uring from its default seccomp profile. **Takeaway for the plan:** treat io_uring as *opt-in, probe-gated, fallback-mandatory*, and never assume presence.

**Privilege / resource requirements:**
- **SQPOLL** — **[plan correction]** the plan says "CAP_SYS_ADMIN/CAP_SYS_NICE pre-5.12." Precise history per `io_uring_setup(2)`: **pre-5.11** SQPOLL required files pre-registered via `IORING_REGISTER_FILES` (else `EBADF`); **5.11** allowed non-root SQPOLL with **`CAP_SYS_NICE`**; **5.13+** needs **no special privilege**. CAP_SYS_ADMIN only re-enters via `io_uring_disabled=1`. Since SQPOLL also burns a full core per ring, keep it **opt-in, off by default** (the plan already does).
- **RLIMIT_MEMLOCK** — `IORING_REGISTER_BUFFERS` pins pages and charges them against the caller's `RLIMIT_MEMLOCK` (per-buffer cap 1 GiB; buffers must be anonymous/non-file-backed; huge pages pinned whole). Over-limit → **ENOMEM**; `CAP_IPC_LOCK` is exempt. Pre-5.13, registering buffers *waits for the ring to idle*. The plan's Phase-2 "detect ENOMEM → fall back to unregistered I/O" is correct; also detect the memlock ceiling up front (`getrlimit`) and size registration to it.

**Required runtime PROBE + graceful FALLBACK (hardened):**
1. Gate on config: only probe if a resolved mode is `io_uring`.
2. **Cheap file check first:** read `/proc/sys/kernel/io_uring_disabled` (if present and `!= 0`, go straight to fallback — avoids even attempting the syscall). This is the safe way to detect the 6.6 sysctl without risking a filtered syscall.
3. **OS/kernel gate:** Linux only; kernel ≥ 5.6 (for `IORING_OP_READ`). Record required feature flags.
4. **Safe setup probe:** attempt `io_uring_setup(1, &params)`; treat **ENOSYS** (not built / too old) and **EPERM/EACCES** (seccomp, sysctl, gVisor) as "unavailable → fall back."
5. **Exercise a real op:** submit one `IORING_OP_READ`/`NOP` against a scratch fd in a data dir and **reap its CQE** — because gVisor and some sandboxes allow `setup` but fail actual ops. Verify golden bytes vs a `pread`.
6. **CRITICAL probe hazard — SIGSYS-on-KILL seccomp:** under `SCMP_ACT_KILL`/`SCMP_ACT_KILL_PROCESS` policies (some systemd `SystemCallFilter` and hardened runtimes), *issuing* `io_uring_setup` **kills the JVM with SIGSYS** rather than returning EPERM. A probe that blindly calls the syscall can crash the node. Mitigate by: (a) the sysctl file check in step 2; (b) preferring an explicit **operator opt-in** (`disk_access_mode: io_uring` is itself the opt-in, good) so we never probe unrequested; and (c) optionally running the first raw probe in a **short-lived forked subprocess** (or a guarded helper) so a SIGSYS kills the probe, not the daemon. Record capability once, never re-probe.
7. **Fallback:** if `io_uring_fallback_on_unavailable` (default true): `logger.warn(...)` and `setDiskAccessMode(standard)` (mirrors `NativeTransportService.useEpoll()` → NIO). Else throw `StartupException`. **[plan alignment]** matches §5.2; add the sysctl-file check and the KILL-seccomp caveat to `checkIoUringAvailability`.

---

## 3. Memory-safety / correctness hazards → mitigation (wrapper-specific)

| # | Hazard | Failure mode | Mitigation |
|---|---|---|---|
| **H1 (CRITICAL)** | **Arena.close()/GC frees memory while a kernel write is still in flight** into that segment | Kernel writes into freed/reused Java memory → heap corruption, torn reads, cross-request data leak, JVM crash | The wrapper must replicate tokio-uring's ownership contract *manually* (Java has no compile-time enforcement): keep every in-flight buffer in an **in-flight registry keyed by `user_data` that owns the `MemorySegment`**; do **not** return the buffer to `BufferPool` or `close()` its Arena until the CQE for that `user_data` is reaped. Use one **long-lived shared Arena** for ring+buffers, never a per-op confined Arena you close on return. `Arena.close()` only after the in-flight registry is empty (see H10). This is the single most important invariant. |
| **H2** | **Heap `ByteBuffer` address instability** (`MemorySegment.ofBuffer(heapBB).address()`) | GC relocates the backing array; kernel DMAs to a stale address | Only ever hand the kernel **off-heap/native** segments (direct `ByteBuffer`/native `MemorySegment`). Cassandra `BufferPool` gives direct buffers — assert it. Add a `reachabilityFence(segment)` spanning the `io_uring_enter` downcall regardless. |
| **H3** | **SQE mutated/reused before the kernel consumes it** | Kernel reads a half-written or overwritten SQE → wrong op/offset/len | Fill the SQE completely, then publish the SQ tail with a **release** store; only reuse an SQE slot after `sq_head` (acquire) has advanced past it. Never touch an SQE after bumping tail until the ring has consumed it. |
| **H4** | **CQ overflow drops completions** | Future never completes → read hangs → query timeout; or (pre-5.5) silently lost | We require kernel ≥5.6 so **`IORING_FEAT_NODROP` is always present**: kernel keeps an internal overflow list, sets `IORING_SQ_CQ_OVERFLOW`; we must call `io_uring_enter(GETEVENTS)` to flush and expect `-EBUSY` on submit until drained. **Size CQ ≥ 2× SQ** (`IORING_SETUP_CQSIZE`) and cap in-flight (H_backpressure) so overflow can't happen in steady state. Assert `NODROP` in the feature check. |
| **H5** | **`user_data` correlation collision / recycling** | A late/duplicate CQE routes to the wrong (recycled) request → data delivered to wrong reader | Use a **monotonic 64-bit counter** (never recycled; 2^64 won't wrap). Don't free a `user_data` slot until its CQE is seen. Account for **multishot** ops (`IORING_CQE_F_MORE` → more CQEs coming, don't retire) — though the read path should use one-shot only. |
| **H6** | **SQ is single-producer; multi-thread submit to one ring** | Two threads race the same SQE slot / tail → corrupted submissions | **One ring per submitting thread**, or confine submission to the ring owner and have foreign threads enqueue on an **MPSC** the owner drains (tokio-uring's exact rationale; PanamaUring's `inEventLoop()?run:execute` pattern). The plan's "ring per carrier + foreign hand-off queue" is correct. |
| **H7** | **fd lifetime vs in-flight ops / fd reuse** | Closing an SSTable fd with pending reads: op completes `-ECANCELED`, **or worse the fd number is reused and the read hits the wrong file** | Never `close()` an fd until all its in-flight ops complete, **or** use `IORING_REGISTER_FILES` + `IOSQE_FIXED_FILE` so the ring holds a stable file reference decoupled from the raw fd (Phase 2). Track per-fd in-flight count; defer close. |
| **H8** | **Cancellation race (ASYNC_CANCEL vs completion)** | Cancel "succeeds" but kernel still writes the buffer (op and cancel are concurrent) → data loss / buffer reused mid-write | **Best-effort cancel is asynchronous** (tokio-uring & PanamaUring both stress this). On cancel, **keep the buffer pinned until the *original* op's terminal CQE arrives** (which will be `-ECANCELED` *or* a real result). Free only then. Never free on cancel-submit. |
| **H9** | **Short read / partial completion (`res < len`)** | Treated as full read → buffer tail is stale/garbage → CRC/deserialization corruption | `res == 0` ⇒ EOF; `0 < res < len` is legal (O_DIRECT near EOF, interrupts) ⇒ **resubmit for the remainder** at `off+res`; `res < 0` ⇒ `-errno`. Trim the buffer limit to real `res`. Mirror the existing rebuffer partial-fill loop. |
| **H10** | **Teardown/close with in-flight ops** | munmap/close-fd while kernel has pending writes → H1 at shutdown | Shutdown order: stop accepting new reads → **drain**: submit nothing new, loop `io_uring_enter(GETEVENTS)` until in-flight registry empties (bounded timeout, then `ASYNC_CANCEL` remaining and await their terminal CQEs) → close ring fd → **munmap** → **`Arena.close()`** last → unregister JMX. Never munmap before drain. |
| **H11** | **`io_uring_enter` EINTR / EAGAIN / EBUSY** | Spurious failure treated as fatal; or busy-loop | `EINTR` → retry; `EAGAIN` → retry (transient resource); `EBUSY` → **reap CQEs first** (CQ-overflow backpressure) then retry submit. Use `Linker.Option.captureCallState("errno")` and a small retry policy. |
| **H12** | **O_DIRECT alignment faults** | Unaligned offset/length/**buffer address** → `EINVAL` at submit | Align all three to logical block size (512 or 4096) via `FileUtils.getBlockSize()` + Agrona `allocateDirectAligned`/`align`. **[plan note]** `BufferPool`'s 2 KiB unit is 512- but not guaranteed 4096-aligned — use the aligned allocator for the DMA buffer or teach the pool a 4096 unit (plan §3.4 already flags this). |
| **H13** | **Linked-op error propagation (`IOSQE_IO_LINK`)** | Downstream op returns `-ECANCELED` because the head failed; misread as an I/O error | Treat `-ECANCELED` on a linked/downstream op as "upstream failed, not corruption"; surface the *head's* errno. The read path likely won't use links initially — if not, don't set `IOSQE_IO_LINK` at all. |
| **H14** | **Registered-buffer index reuse / RLIMIT_MEMLOCK (Phase 2)** | Wrong `buf_index` → reads into the wrong slab; over-registration → ENOMEM | Stable index→segment mapping for the ring's lifetime; detect memlock ceiling and cap; fall back to unregistered `IORING_OP_READ` on ENOMEM. |
| **H15** | **FFM downcall pinning a virtual thread** | Blocking *inside* the native frame won't unmount the carrier | Keep `io_uring_enter` **non-blocking at submit** (submit-only, `min_complete=0`); do the *waiting* on plain Java `future.get()` off the downcall (unmounts cleanly on JDK 25 post-JEP 491). Never block inside an FFM frame. **[plan alignment]** §2 Strategy C. |

The recurring theme across H1/H8/H10: **the kernel can write into your memory after your Java code thinks it's done.** tokio-uring encodes this in the type system; PanamaUring encodes it in an explicit ownership/rent object and memory-safe cancel. The Cassandra library has *neither* for free and must enforce it with an owning in-flight registry + drain-on-close.

---

## 4. Testing strategy — what these libraries do + what Cassandra should adopt

Observed practice: nio_uring/jasyncfio/JUring are **benchmark-heavy, correctness-light** (JMH matrices, few systematic fault tests) — JUring's large JMH grid (buffer size × threads × registered/unregistered, QD 256) is the model for perf, but none ship a rigorous safety suite. Cassandra must go further:

- **Deterministic binding unit tests (golden-pread oracle):** read the same byte ranges from a **tmpfs/loopback** file via `FileChannel.read` and via the ring; assert byte-for-byte equality across offsets/lengths/EOF/>4 GiB. This is the differential oracle *at the binding level* and catches ABI/layout mistakes immediately. Run on tmpfs (fast, deterministic) and a real block device (O_DIRECT path).
- **Property / fuzz tests:** use Cassandra's existing `qt()`/quicktheories to generate **random op sequences** — mixed offsets/lengths, interleaved submit/reap, forced short reads, injected cancellations, ring-full — and assert the golden oracle + no lost/duplicated `user_data`.
- **Fault injection:** `EINVAL` via deliberate misalignment; `ENOMEM` via `setrlimit(RLIMIT_MEMLOCK, small)` before buffer registration; **unavailable-ring** via `kernel.io_uring_disabled=2` (in a namespaced test) *and* a programmable `FakeIoUringRing` returning specific `-errno`, partial fills, and `IORING_SQ_CQ_OVERFLOW`; negative CQE (`EIO`) → `FSReadError`; CRC mismatch → `CorruptSSTableException`.
- **Stress / soak:** sustained high queue depth, many rings, long duration; assert steady-state correctness and that **in-flight registry, fd count (`/proc/self/fd`), and native memory return to baseline**.
- **Leak detection (the Java-specific must-have):** at teardown assert in-flight registry size == 0; track `Arena`/segment allocation vs free; watch `/proc/self/fd` and RSS; a growing user_data map is a lost-completion (H4) signal.
- **Cross-arch:** run CI on **x86-64 and aarch64** (Cassandra's asahi/aarch64 host makes this natural). Note: raw io_uring syscall numbers are **425/426/427 on both** x86-64 and aarch64, so the risk isn't the numbers — it's **struct layout / padding / cache-line alignment** of `io_uring_params`/`sqe`/`cqe`; assert `MemoryLayout.byteSize()` against the kernel ABI on each arch.
- **Native-memory "sanitizer-equivalent":** FFM has no MSAN, but (a) `MemorySegment` bounds-checking already catches OOB Java-side; (b) run the golden-oracle suite under a debug build to catch H1/H9; (c) build a small C reproduction under **ASan/valgrind** for the raw ABI as a cross-check; (d) run with FFM restricted-method checks enabled (`--enable-native-access` scoping). H1 (write-after-free) is best caught by a **poisoned-buffer test**: fill the buffer with a sentinel, free-then-reallocate aggressively while ops are "in flight" against a `FakeRing` that writes late, and assert no sentinel corruption escapes.
- **Differential oracle at the Cassandra level:** Harry (`HarryIoUringReadTest`) running identical history under `io_uring` vs `standard` — any model divergence = bug. Parameterize existing reader suites across access modes. (Plan §7 already specifies this; keep it.)
- **Simulator:** io_uring completions arrive off the deterministic scheduler → **keep the simulator on `standard`** and assert io_uring is never requested under simulation. (Plan §7.6.)

---

## 5. Concurrency model — production io_uring users → Cassandra mapping

**How the reference systems do it:**
- **ScyllaDB / Seastar** — **shard-per-core, share-nothing.** One reactor + one io_uring **per shard pinned to a core**; data is partitioned per shard so there is *no cross-core submission* and thus no SQ contention. Seastar runs its **own userspace I/O scheduler (fair queue)** that bounds in-flight requests per I/O class, on top of O_DIRECT + its own page cache. Their measured io_uring win over a *well-tuned* linux-aio was small (~5% on 512 B Optane reads) — a direct caution that Gate 0 must be honest.
- **tokio-uring** — **thread-per-core, one ring per runtime thread, no work-stealing.** Operation futures and buffers are **`!Send`** (they must stay on the ring's thread to receive the CQE). Multi-thread support = **one SQ/CQ pair per thread**, not a shared ring, precisely because "the SQ's single-producer characteristic optimizes for a single thread." Buffers use the **pass-ownership/rent** model; cancel on drop is best-effort and the runtime keeps the buffer alive until the real completion.
- **Glommio** — thread-per-core cooperative executor (Seastar-modeled), **one ring per executor thread**, latency/placement classes over io_uring for both file and net.

**Common law across all three: never share one ring's SQ across threads; pin a ring to a thread; bound in-flight depth.**

**Mapping onto Cassandra (which is *not* shard-per-core):**
- Cassandra's `ReadStage` is a **shared `SEPExecutor`**, data is **not partitioned per core**, and prior thread-per-core (TPC) read-path rewrites never merged. So **do not** adopt Seastar's shard-per-core wholesale — that's the Strategy-A-scale rewrite the plan explicitly defers.
- **Recommended: thread-per-ring, not ring-per-core-with-affinity.** Run a **bounded pool of N submit/poll threads** (N ≈ `concurrent_reads` or CPU count), each owning **one ring** (its own SQ+CQ, single-producer/single-consumer). Use **virtual threads purely as the suspension vehicle** (plan Strategy C): the read task, running on a VThread, calls `future.get()` and unmounts its carrier; the carrier/poller does the submit+reap. **Foreign submitters hand off via an MPSC queue** the ring owner drains (tokio-uring/PanamaUring rationale) — never submit to a ring you don't own.
- **Do NOT create one ring per virtual thread** (millions of short-lived VThreads). Keep rings ∝ carriers, and **keep buffer allocation on the bounded carrier layer** to avoid `FastThreadLocal`/`BufferPool.LocalPool` thrash from ephemeral VThreads (plan §2 caveat, §5.3 — keep it).
- **SQPOLL: off by default.** It burns a full core per ring and (pre-5.13) needs CAP_SYS_NICE — wasteful for Cassandra's bursty, cache-absorbed read pattern. Offer as opt-in only for dedicated, I/O-saturated nodes.
- **Backpressure:** a per-ring `Semaphore(queue_depth)` acquired before submit / released on CQE, with CQ sized ≥ 2× SQ, keeps in-flight bounded and CQ-overflow (H4) impossible in steady state.
- **Wakeup:** if a poller blocks in `io_uring_enter(min_complete>0)`, wake it from foreign threads via an **eventfd registered as an SQE** (PanamaUring's JDK-Selector-style trick), **not** `IORING_OP_NOP` (which reintroduces a completion race).

---

## 6. Gaps the standalone library should fill (vs all existing bindings)

1. **Pure-FFM, zero native artifact** — raw `syscall(425/426/427)` via `Linker.nativeLinker()`, **no liburing dependency** (unlike JUring) and **no JNI `.so`** (unlike Netty/nio_uring/jasyncfio). Only PanamaUring is comparable, and it isn't ASF-shippable.
2. **File-read-minimal opcode set** — `READ`/`READV`/`READ_FIXED`(+`FSYNC`), not a general socket/net framework. Small, auditable ABI surface.
3. **First-class availability probe + graceful fallback** — sysctl-file check, exercise-a-real-op probe, **SIGSYS/KILL-seccomp-safe** probing, one-shot capability cache. No existing Java binding does this; it is the #1 operational requirement (§2).
4. **Runtime-enforced memory-safety contract** — an owning **in-flight registry** (buffer pinned until terminal CQE), **memory-safe cancel** (buffer freed only on the original op's CQE), and **drain-before-close** teardown. Replaces the compile-time guarantee Rust libraries get for free; existing Java libs are weaker here.
5. **Bounded backpressure + CQ-overflow-proof sizing** built in (semaphore = queue depth, CQ ≥ 2× SQ, `NODROP` asserted).
6. **Registered buffers/files with RLIMIT_MEMLOCK detection and automatic fallback** to unregistered I/O on ENOMEM.
7. **Cross-arch (x86-64 + aarch64) with ABI-layout assertions** and a **deterministic `FakeRing`** for host-side, kernel-free unit/property/fault tests.
8. **Thread-per-ring concurrency with a foreign-submitter MPSC hand-off and eventfd wakeup** — safe by construction against the single-producer-SQ hazard, and designed to sit under Loom virtual threads (suspension vehicle) rather than shard-per-core.
9. **Observability** — JMX metrics for in-flight depth, CQ-overflow events, fallback state, submit/reap latency — so operators can confirm batching (`avgqu-sz ↑`) and detect silent fallback.
10. **License/packaging clean for ASF** — Apache-2.0, no new binary dependency to vet or ship per-arch, JDK 25 baseline, guarded so JDK-17 CI compiles cleanly (Linux+JDK25-only runtime paths).

---

## Sources

- Netty io_uring incubator (archived, merged into 4.2): https://github.com/netty/netty-incubator-transport-io_uring — Netty 4.2 release: https://netty.io/news/2025/03/26/4-2-0-Final.html
- nio_uring (`bbeaupain/nio_uring`): https://github.com/bbeaupain/nio_uring
- jasyncfio (`ikorennoy/jasyncfio`): https://github.com/ikorennoy/jasyncfio
- JUring (`davidtos/JUring`): https://github.com/davidtos/JUring
- PanamaUring (`dreamlike-ocean/PanamaUring`): https://github.com/dreamlike-ocean/PanamaUring
- tokio-uring DESIGN.md (thread-per-core, `!Send`, ownership/rent, best-effort cancel): https://github.com/tokio-rs/tokio-uring/blob/master/DESIGN.md
- `kernel.io_uring_disabled` sysctl (values 0/1/2, io_uring_group): https://docs.kernel.org/admin-guide/sysctl/kernel.html
- Docker default seccomp blocks io_uring (moby#46762): https://docs.docker.com/engine/security/seccomp/
- gVisor io_uring compatibility (disabled-by-default, partial): https://gvisor.dev/docs/user_guide/compatibility/
- io_uring security / Google 2023 60% figure, Android/ChromeOS/server disable, Docker seccomp removal: https://en.wikipedia.org/wiki/Io_uring ; Google kCTF learnings (June 2023): https://security.googleblog.com/2023/06/learnings-from-kctf-vrps-42-linux.html
- SQPOLL privilege history (CAP_SYS_NICE 5.11, none 5.13), EPERM/io_uring_disabled: https://man7.org/linux/man-pages/man2/io_uring_setup.2.html
- RLIMIT_MEMLOCK / IORING_REGISTER_BUFFERS accounting, ENOMEM, CAP_IPC_LOCK: https://man7.org/linux/man-pages/man2/io_uring_register.2.html
- io_uring(7) — user_data correlation, SQ single-producer indirection, SQPOLL pointer-lifetime: https://man7.org/linux/man-pages/man7/io_uring.7.html
- ScyllaDB on io_uring (share-nothing, DIO, ~5% Optane win): https://www.scylladb.com/2020/05/05/how-io_uring-and-ebpf-will-revolutionize-programming-in-linux/

Plan file reviewed: `/home/cscotta/projects/cassandra/IO_URING_DISK_ACCESS_MODE_PLAN.md`. Key corrections to fold in: (a) SQPOLL privilege boundary is 5.11 `CAP_SYS_NICE` / 5.13 none, not "pre-5.12"; (b) add `/proc/sys/kernel/io_uring_disabled` file check + SIGSYS/KILL-seccomp-safe probing to `checkIoUringAvailability` (§5.2); (c) make the availability probe *exercise a real op* (gVisor allows setup, fails ops); (d) elevate H1/H8/H10 (kernel-writes-after-free / cancel keeps buffer pinned / drain-before-munmap) to first-class invariants enforced by an owning in-flight registry.