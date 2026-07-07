I have complete, empirically-verified data from JDK 25.0.3 on this aarch64/Fedora machine (glibc 2.43, kernel 7.0). Here is the reference.

---

# JDK 25 FFM binding reference for a Linux `io_uring` layer

**Verified on this box:** OpenJDK 25.0.3 (Red Hat build), `aarch64`, kernel 7.0, glibc 2.43. Every snippet below was compiled and run; behavioral claims marked *(verified)* were observed directly, not recalled. `syscall`, `mmap`, `munmap` are all present in `Linker.nativeLinker().defaultLookup()` *(verified)*. io_uring syscall numbers on this kernel: **`io_uring_setup=425`, `io_uring_enter=426`, `io_uring_register=427`** — these are the `asm-generic` numbers shared by `aarch64` **and** `x86-64` *(verified from `/usr/include/asm-generic/unistd.h`)*. Note the dev box is aarch64, so `SYS_getpid=172`, `SYS_close=57`, whereas x86-64 uses 39/3 — but you never need those; only 425/426/427 matter and they're arch-agnostic here.

## Corrections to the existing `IO_URING_DISK_ACCESS_MODE_PLAN.md`

- **§3.6 / §5.6 overstate the flag.** `--enable-native-access=ALL-UNNAMED` is **not strictly mandatory on JDK 25** — FFM downcalls *run* under the default policy, emitting a one-time warning per module. It **is** required to (a) silence the warning and (b) survive the eventual default flip to `deny`. Precise wording below (§9).
- **§3.2 "use `VarHandle.getAcquire()/setRelease()`"** is correct and now confirmed: segment/layout `VarHandle`s support the full access-mode set including `GET_ACQUIRE`/`SET_RELEASE`/`GET_AND_ADD`/CAS *(verified)*. This was the open question — answer in §7.
- **§2 Strategy C pinning note:** a normal (non-`critical`) FFM downcall performs a Java→native thread-state transition, so a virtual thread that *blocks inside* a blocking `io_uring_enter` will pin its carrier for the syscall's duration. The plan's design (poll CQEs from ring memory, wait via `future.get()`) correctly avoids blocking in native — keep it that way. Do **not** wrap a blocking `io_uring_enter` (min_complete>0) in `critical` (§11).
- **`Linker.Option.isTrivial` no longer exists** — it's `Linker.Option.critical(boolean allowHeapAccess)` *(verified in `javap`)*.

---

## 1. Linker, downcall handles, and binding variadic `syscall`

`Linker` API surface on JDK 25 *(verified via `javap`)*:

```java
Linker nativeLinker();
MethodHandle downcallHandle(MemorySegment addr, FunctionDescriptor fd, Linker.Option... opts);
MethodHandle downcallHandle(FunctionDescriptor fd, Linker.Option... opts); // address bound at call time (extra leading MemorySegment)
SymbolLookup defaultLookup();
Map<String,MemoryLayout> canonicalLayouts();   // "size_t"->j8, "long"->j8, "int"->i4 on this box (verified)
```

`Linker.Option`: `firstVariadicArg(int)`, `captureCallState(String...)`, `captureStateLayout()`, `critical(boolean)` *(verified)*.

**Recommended pattern — one fixed-arity handle per io_uring syscall, all routed through libc `syscall(long number, ...)`.** Prepend the arch syscall number as the first `long`; declare everything after it variadic with `firstVariadicArg(1)`. `firstVariadicArg` is required even on Linux where integer varargs share GP registers, because the linker uses it to set up the ABI correctly (e.g. zeroing `%al` on x86-64).

```java
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import static java.lang.foreign.ValueLayout.*;

final class IoUringSyscalls {
    // asm-generic numbers: identical on aarch64 and x86-64 (verified)
    static final long SYS_io_uring_setup    = 425;
    static final long SYS_io_uring_enter     = 426;
    static final long SYS_io_uring_register  = 427;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final MemorySegment SYSCALL = LINKER.defaultLookup().findOrThrow("syscall");

    // int io_uring_setup(u32 entries, struct io_uring_params *p)
    //   -> syscall(425, entries, params_ptr)
    static final MethodHandle IO_URING_SETUP = LINKER.downcallHandle(
        SYSCALL,
        FunctionDescriptor.of(JAVA_LONG,   // return (ring fd, or -1)
                              JAVA_LONG,   // syscall number
                              JAVA_INT,    // entries
                              ADDRESS),    // io_uring_params*
        Linker.Option.firstVariadicArg(1),
        Linker.Option.captureCallState("errno"));

    // int io_uring_enter(u32 fd, u32 to_submit, u32 min_complete, u32 flags,
    //                    const sigset_t *sig, size_t sigsz)
    static final MethodHandle IO_URING_ENTER = LINKER.downcallHandle(
        SYSCALL,
        FunctionDescriptor.of(JAVA_LONG,
                              JAVA_LONG,   // number = 426
                              JAVA_INT,    // fd
                              JAVA_INT,    // to_submit
                              JAVA_INT,    // min_complete
                              JAVA_INT,    // flags (IORING_ENTER_GETEVENTS = 1)
                              ADDRESS,     // sigset_t* (MemorySegment.NULL)
                              JAVA_LONG),  // sigsz (0)
        Linker.Option.firstVariadicArg(1),
        Linker.Option.captureCallState("errno"));

    // int io_uring_register(u32 fd, u32 opcode, void *arg, u32 nr_args)
    static final MethodHandle IO_URING_REGISTER = LINKER.downcallHandle(
        SYSCALL,
        FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT),
        Linker.Option.firstVariadicArg(1),
        Linker.Option.captureCallState("errno"));
}
```

Call sites use `invokeExact` (never `invoke`) so no boxing/`asType` on the hot path. The first `MemorySegment` argument is the errno-capture segment (§2); the second is the syscall number:

```java
long fd = (long) IO_URING_SETUP.invokeExact(errnoSeg, SYS_io_uring_setup, entries, paramsSeg);
```

A `syscall(SYS_getpid)` round-trip was verified end-to-end (returned the same value as `ProcessHandle.current().pid()`), as was a blocking-return handle plus a `critical` handle.

---

## 2. errno capture

`captureStateLayout()` returns a platform `StructLayout` that on Linux contains a member named `"errno"`. `captureCallState("errno")` adds a **leading** `MemorySegment` parameter to the handle; the linker writes `errno` into that segment immediately after the call (before any JVM code can clobber it). *(Verified: `close(-1)` returned `-1` with captured `errno=9` = `EBADF`.)*

```java
static final StructLayout CAPTURE = Linker.Option.captureStateLayout();
static final VarHandle ERRNO_VH =
    CAPTURE.varHandle(MemoryLayout.PathElement.groupElement("errno"));  // coords: (MemorySegment, long)

// Reuse ONE capture segment per ring/thread (confined) — do not allocate per call.
MemorySegment errnoSeg = ringArena.allocate(CAPTURE);

long ret = (long) IoUringSyscalls.IO_URING_ENTER.invokeExact(
        errnoSeg, IoUringSyscalls.SYS_io_uring_enter,
        ringFd, toSubmit, minComplete, flags, MemorySegment.NULL, 0L);
if (ret < 0) {
    int errno = (int) ERRNO_VH.get(errnoSeg, 0L);   // note the trailing 0L base offset
    throw new IOException("io_uring_enter failed, errno=" + errno);
}
```

**Note for io_uring specifically:** you only need errno for the three control syscalls plus `mmap`. Per-I/O results do **not** use errno — the CQE `res` field carries `-errno` directly (§6). So errno capture is off the data hot path entirely.

---

## 3. SymbolLookup

*(All verified present in `defaultLookup()`.)*

```java
SymbolLookup libc = Linker.nativeLinker().defaultLookup();     // libc/libm already loaded into the linker
MemorySegment syscall = libc.findOrThrow("syscall");           // Optional-throwing variant
Optional<MemorySegment> mmap = libc.find("mmap");              // present == true (verified)

// Optional C shim fallback (§ plan Option B): libcassandra_uring.so with named wrappers.
// The Arena governs the library's unload; use a long-lived arena (global or manager-scoped).
try (Arena libArena = Arena.ofShared()) {
    SymbolLookup shim = SymbolLookup.libraryLookup("cassandra_uring", libArena); // or Path form
    SymbolLookup all  = shim.or(libc);   // compose: try shim first, fall back to libc
}

// If the .so was loaded via System.loadLibrary in the caller's loader:
SymbolLookup mine = SymbolLookup.loaderLookup();
```

`libraryLookup(String|Path, Arena)` is itself a restricted method; the returned symbols are valid only while `libArena` is alive. For a shim you want alive for the JVM lifetime, use `Arena.global()`.

---

## 4. Arena types and thread confinement

*(API verified: `global()`, `ofAuto()`, `ofConfined()`, `ofShared()`; `Arena extends AutoCloseable`; `MemorySegment.isAccessibleBy(Thread)`.)*

| Arena | Access | Close | Use for io_uring |
|---|---|---|---|
| `ofConfined()` | **only the creating thread** | explicit, same thread | A ring whose submit **and** reap happen on one thread; per-thread errno/params scratch. Fastest checks. |
| `ofShared()` | any thread | explicit, via global handshake | A ring mmap'd region **accessed by multiple threads** (foreign submitters hand off, or a poller thread distinct from submitters). |
| `ofAuto()` | any thread | GC-driven (no `close`) | Long-lived scratch you don't want to lifecycle-manage; can't deterministically munmap → not ideal for ring memory. |
| `global()` | any thread | never | The C shim library lookup; process-lifetime registered-buffer tables. |

**Recommendation per the plan's "one ring per carrier, submit+reap on that thread" model:** wrap each ring's SQ/CQ/SQE mmaps and its scratch segments in **`ofConfined()` created on that carrier thread**, and `close()` it (running munmap via the reinterpret cleanup, §5) during ring teardown on that same thread. If you instead let arbitrary threads submit into a shared ring (the "foreign submitters hand off via a queue" variant), the ring segments **must** be `ofShared()` — a confined segment throws `WrongThreadException` on cross-thread access.

**Per-request I/O buffers:** don't allocate them from a per-request arena. Take them from the existing `BufferPool` (direct `ByteBuffer`s) and wrap with `MemorySegment.ofBuffer(bb)` (§5) — that segment is thread-agnostic and its lifetime tracks buffer reachability, which is what you want when submit/complete/consume span three threads.

**Use-after-close is the sharp edge:** any access to a segment from a *closed confined/shared arena* throws `IllegalStateException`. For `ofShared`, `close()` performs a thread handshake and will not return until no other thread is mid-access — but it will happily invalidate segments other threads are *about to* use. Enforce the invariant that a ring's arena is closed **only after** the in-flight map is drained (all CQEs reaped, all buffers released).

---

## 5. MemorySegment: mmap regions and direct-buffer addresses

*(Signatures verified.)*

```java
// (a) Wrap an mmap'd ring region. ofAddress(addr) yields a zero-length segment;
//     reinterpret gives it a size, ties validity to `arena`, and registers munmap as cleanup.
MemorySegment sqRing = MemorySegment.ofAddress(mmapAddr)
        .reinterpret(sqRingBytes, ringArena, seg -> munmap(seg, sqRingBytes));

// (b) Obtain a direct ByteBuffer's native address to place in an SQE.
ByteBuffer direct = bufferPool.get(len);           // off-heap, address stable under GC
MemorySegment bufSeg = MemorySegment.ofBuffer(direct);   // covers [position, limit)
long bufAddr = bufSeg.address();                    // write into sqe.addr

// (c) Typed struct field access.
int res = cqeSeg.get(JAVA_INT, RES_OFFSET);
sqeSeg.set(JAVA_LONG, USER_DATA_OFFSET, token);
sqeSeg.asSlice(0, 64).fill((byte) 0);               // zero an SQE before filling
```

`reinterpret` overloads: `reinterpret(long)`, `reinterpret(Arena, Consumer)`, `reinterpret(long, Arena, Consumer)` — the third is the one for mmap. **Alignment:** the aligned accessors (`JAVA_INT`, etc.) throw `IllegalArgumentException` on a misaligned offset *(verified)*; use `JAVA_INT_UNALIGNED`/`JAVA_LONG_UNALIGNED` if a field lands off its natural boundary (io_uring's ring fields are naturally aligned, so this shouldn't arise — but computed offsets from `io_sqring_offsets` should be sanity-checked). `reinterpret` itself is a restricted method.

---

## 6. MemoryLayout / StructLayout — modeling the ABI

*(API verified: `structLayout`, `unionLayout`, `sequenceLayout`, `paddingLayout`, `PathElement.groupElement(String|long)`, `sequenceElement(...)`, `varHandle(...)`, `byteOffsetHandle(...)`, `byteOffset(...)`.)*

**Critical rule: `structLayout` does NOT auto-pad.** Each member must sit at its natural alignment or the layout is rejected (`IllegalArgumentException`); insert `MemoryLayout.paddingLayout(n)` explicitly to match the C ABI. Trailing padding to a struct's size multiple is also your responsibility.

```java
// struct io_uring_cqe { __u64 user_data; __s32 res; __u32 flags; }  (16 bytes)
static final StructLayout CQE = MemoryLayout.structLayout(
        JAVA_LONG.withName("user_data"),
        JAVA_INT.withName("res"),
        JAVA_INT.withName("flags"));
// verified: byteOffset(res)=8, byteSize=16

// struct io_sqring_offsets { u32 head,tail,ring_mask,ring_entries,flags,dropped,array; u32 resv1; u64 resv2; } (40 B)
static final StructLayout SQ_OFFSETS = MemoryLayout.structLayout(
        JAVA_INT.withName("head"), JAVA_INT.withName("tail"),
        JAVA_INT.withName("ring_mask"), JAVA_INT.withName("ring_entries"),
        JAVA_INT.withName("flags"), JAVA_INT.withName("dropped"),
        JAVA_INT.withName("array"), JAVA_INT.withName("resv1"),
        JAVA_LONG.withName("resv2"));

// struct io_uring_params (120 B): 7 u32 + u32 resv[3] + sq_off(40 @40) + cq_off(40 @80)
static final StructLayout PARAMS = MemoryLayout.structLayout(
        JAVA_INT.withName("sq_entries"), JAVA_INT.withName("cq_entries"),
        JAVA_INT.withName("flags"), JAVA_INT.withName("sq_thread_cpu"),
        JAVA_INT.withName("sq_thread_idle"), JAVA_INT.withName("features"),
        JAVA_INT.withName("wq_fd"),
        MemoryLayout.sequenceLayout(3, JAVA_INT).withName("resv"),   // 12 B -> offset 40
        SQ_OFFSETS.withName("sq_off"),                                // @40
        CQ_OFFSETS.withName("cq_off"));                               // @80

// io_uring_sqe is 64 B with anonymous unions -> model the union arm you use
// (IORING_OP_READ) and pad the rest to 64. Use unionLayout for the overlapping fields.

// Deriving accessors:
static final VarHandle FEATURES =
        PARAMS.varHandle(MemoryLayout.PathElement.groupElement("features"));   // (MemorySegment, long)
static final long SQ_TAIL_OFF =
        PARAMS.byteOffset(MemoryLayout.PathElement.groupElement("sq_off"),
                          MemoryLayout.PathElement.groupElement("tail"));
```

`select(...)` extracts a sub-layout; `arrayElementVarHandle(...)` and `sequenceElement()` handle the `sq_array[]` index array; `byteOffsetHandle(...)` gives a `MethodHandle` for computed offsets (useful when the ring index varies).

---

## 7. Memory ordering — `smp_load_acquire` / `smp_store_release` on ring head/tail  ★ (the key question)

**Answer: segment/value `VarHandle`s support the full concurrent access-mode set, and `getAcquire`/`setRelease` are the exact FFM equivalents of liburing's `io_uring_smp_load_acquire`/`io_uring_smp_store_release`** (which are `__atomic_load_n(_, __ATOMIC_ACQUIRE)` / `__atomic_store_n(_, __ATOMIC_RELEASE)`).

*(Verified on 25.0.3:* a `VarHandle` from a layout supports `GET`, `SET`, `GET_VOLATILE`, `SET_VOLATILE`, `GET_ACQUIRE`, `SET_RELEASE`, `GET_OPAQUE`, `SET_OPAQUE`, `COMPARE_AND_SET`, `COMPARE_AND_EXCHANGE(_ACQUIRE/_RELEASE)`, `GET_AND_SET`, `GET_AND_ADD`, and all bitwise variants — an acquire/release round-trip was executed against a shared-arena segment.*)

**Use a standalone `JAVA_INT.varHandle()`, not a struct-path handle**, because head/tail live at offsets discovered at runtime from `io_sqring_offsets`/`io_cqring_offsets`, not at compile-time struct positions. Its coordinates are `[MemorySegment, long]` *(verified)*.

```java
// One shared handle for all 32-bit ring counters.
static final VarHandle U32 = JAVA_INT.varHandle();      // coords: (MemorySegment, long offset)

// ---- SUBMIT (producer): publish SQEs, then bump SQ tail with release ----
// 1. write the SQE fields (plain stores)
// 2. write sq_array[tail & mask] = sqeIndex (plain)
// 3. release-store the new tail so the kernel sees SQE+array writes first:
int tail = (int) U32.getOpaque(sqRing, sqTailOff);      // our own tail; plain/opaque read is fine
U32.setRelease(sqRing, sqTailOff, tail + n);            // == io_uring_smp_store_release(&sq.tail, ...)
// then io_uring_enter(to_submit = n, ...)

// ---- REAP (consumer): acquire-load CQ tail, process, release-store CQ head ----
int cqHead = (int) U32.getOpaque(cqRing, cqHeadOff);    // our own head
int cqTail = (int) U32.getAcquire(cqRing, cqTailOff);   // == io_uring_smp_load_acquire(&cq.tail)
while (cqHead != cqTail) {
    long slot = (cqHead & cqRingMask);
    MemorySegment cqe = cqesSeg.asSlice(slot * 16, 16);
    long userData = cqe.get(JAVA_LONG, 0);
    int  res      = cqe.get(JAVA_INT, 8);               // res<0 => -errno
    dispatchCompletion(userData, res);
    cqHead++;
}
U32.setRelease(cqRing, cqHeadOff, cqHead);              // == io_uring_smp_store_release(&cq.head, ...)
```

**Why acquire/release and not volatile:** they generate exactly the barriers the kernel's shared-memory protocol needs (LoadLoad+LoadStore on the acquire load of `cq.tail`; StoreStore+LoadStore before the release store of `sq.tail`/`cq.head`) without the full StoreLoad fence that `getVolatile`/`setVolatile` would impose — matching liburing on aarch64 (which is a weak memory model where this actually matters) and x86-64. `VarHandle.acquireFence()`/`releaseFence()`/`fullFence()` are available as standalone barriers if you ever need a fence decoupled from a specific access (e.g. the `fullFence()` + re-read of `sq.flags`/`IORING_SQ_NEED_WAKEUP` dance under SQPOLL), but prefer per-access acquire/release modes elsewhere. Do **not** use plain `get`/`set` for the shared counters.

---

## 8. Upcalls — not needed (documented for completeness)

io_uring delivers completions by writing CQEs into the shared CQ ring, which you poll (§7) — there is **no kernel callback into Java**, so `Linker.upcallStub` is unnecessary for this feature. For the record, the signature is:

```java
MemorySegment upcallStub(MethodHandle target, FunctionDescriptor fd, Arena arena, Linker.Option...);
```

The returned function-pointer segment is valid only while `arena` is alive; a use-after-close crashes the VM. You'd only reach for this if you later bound something callback-driven (e.g. a native library that invokes a Java completion handler) — not the case here.

---

## 9. Restricted methods + `--enable-native-access` on JDK 25 (measured)

**Measured behavior on 25.0.3** (calling the restricted `Linker::downcallHandle` and then invoking a real syscall):

- **Default (no flag):** the call **succeeds** and prints a one-time-per-module warning:
  > `WARNING: A restricted method in java.lang.foreign.Linker has been called`
  > `WARNING: java.lang.foreign.Linker::downcallHandle has been called by … in an unnamed module`
  > `WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module`
  > `WARNING: Restricted methods will be blocked in a future release unless native access is enabled`
  After the first warned call, `getModule().isNativeAccessEnabled()` flips to **`true`** for that module (warn mode retroactively enables it) — so don't use `isNativeAccessEnabled()` as a pre-flight gate; it reads `false` only until the first restricted call.
- **`--enable-native-access=ALL-UNNAMED`:** clean, no warnings; `isNativeAccessEnabled()==true` from the start.
- **`--illegal-native-access=deny`:** throws `java.lang.IllegalCallerException: Illegal native access from an unnamed module` at `downcallHandle` (stack: `Module.ensureNativeAccess` → `Reflection.ensureNativeAccess` → `AbstractLinker.downcallHandle`).

**So on JDK 25 the default is still `warn`, not `deny`.** JEP history: FFM was finalized by **JEP 454 (JDK 22)**; **JEP 472 (JDK 24)** unified the native-access restriction story and added the `--illegal-native-access=allow|warn|deny` flag with **`warn` as the default**, and that default is unchanged in JDK 25. The flip to `deny`-by-default is the "future release" the warning refers to.

**Exact config to add** (Cassandra runs from the classpath = unnamed module, so `ALL-UNNAMED` is right):

- `conf/jvm25-server.options` (and `jvm25-clients.options` if clients do FFM): add
  ```
  --enable-native-access=ALL-UNNAMED
  ```
- **Tests:** the same flag must be on the test JVM's args (the `jvmarg`/`argLine` used by the ant/junit runner and by JMH forks in `build-bench.xml`), or every FFM test forks with the warning and future-deny risk. If you want CI to *prove* the flag is wired, add `--illegal-native-access=deny` to the io_uring test JVM only — it turns a missing `--enable-native-access` into a hard failure instead of a silent warning.
- Because build.xml compiles without `--release` and CI also builds on JDK 17, the FFM classes must be **loaded** only on Linux+JDK25 (guard construction of `IoUringSyscalls`/`Linker` behind the availability probe so JDK17 runs never touch `java.lang.foreign`). Compilation is fine — `java.lang.foreign` is stable API in 22+ — but keep the static `Linker`/lookup init out of any class that loads on the JDK17 path.

---

## 10. mmap / munmap via FFM

Bind the libc wrappers directly (present in `defaultLookup` — verified) rather than `syscall(SYS_mmap)`; the wrapper handles the `off_t` scaling and returns `MAP_FAILED` cleanly.

```java
// void *mmap(void *addr, size_t len, int prot, int flags, int fd, off_t off)
static final MethodHandle MMAP = LINKER.downcallHandle(
    LINKER.defaultLookup().findOrThrow("mmap"),
    FunctionDescriptor.of(ADDRESS,              // returns void* (MAP_FAILED == (void*)-1)
                          ADDRESS, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_LONG),
    Linker.Option.captureCallState("errno"));

// int munmap(void *addr, size_t len)
static final MethodHandle MUNMAP = LINKER.downcallHandle(
    LINKER.defaultLookup().findOrThrow("munmap"),
    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG),
    Linker.Option.captureCallState("errno"));

static final int PROT_READ = 0x1, PROT_WRITE = 0x2;
static final int MAP_SHARED = 0x01, MAP_POPULATE = 0x8000;   // Linux
static final long IORING_OFF_SQ_RING = 0L, IORING_OFF_CQ_RING = 0x8000000L, IORING_OFF_SQES = 0x10000000L;

MemorySegment mmapRing(int ringFd, long len, long offset, MemorySegment errnoSeg) throws IOException {
    MemorySegment r = (MemorySegment) MMAP.invokeExact(errnoSeg,
            MemorySegment.NULL, len, PROT_READ | PROT_WRITE,
            MAP_SHARED | MAP_POPULATE, ringFd, offset);
    if (r.address() == -1L) {                                // MAP_FAILED
        int errno = (int) ERRNO_VH.get(errnoSeg, 0L);
        throw new IOException("mmap failed errno=" + errno);
    }
    return r.reinterpret(len, ringArena, seg -> {            // munmap on arena close
        try { int rc = (int) MUNMAP.invokeExact((MemorySegment) null_errno, seg, len); }
        catch (Throwable t) { throw new RuntimeException(t); }
    });
}
```

With `IORING_FEAT_SINGLE_MMAP` (kernel ≥5.4, check `params.features`) the SQ and CQ rings share one mapping, so you do **two** mmaps (ring + SQEs) instead of three. `MAP_POPULATE` prefaults the ring pages so the first submit doesn't fault.

---

## 11. Performance: downcall cost, `critical`, avoiding allocation

- **Downcall overhead:** a normal downcall does a Java→native thread-state transition (so GC/safepoints work while native runs) plus argument shuffling — on the order of tens of ns. The dominant io_uring win is **batching many SQEs per `io_uring_enter`**, which amortizes this to near-zero per I/O; don't micro-optimize the single call before batching.
- **`Linker.Option.critical(boolean allowHeapAccess)`** links a handle that **skips the thread-state transition** (big relative saving on a trivial call) but the native function **must return almost immediately and must not block**, because the thread cannot be safepointed for the call's duration — a blocking call under `critical` stalls GC for every other thread.
  - **`io_uring_enter` with `min_complete>0` / `IORING_ENTER_GETEVENTS`: never `critical`** — it blocks in the kernel.
  - A **submit-only** `io_uring_enter(to_submit=n, min_complete=0, flags=0)` returns quickly and is a *candidate*, but it's still a syscall that can occasionally take microseconds; given the safepoint-stall risk and that batching already dominates, **recommend a normal downcall** and reserve `critical(true)` for genuinely trivial, guaranteed-non-blocking helpers only.
  - **`allowHeapAccess=true` is irrelevant and dangerous for io_uring data buffers**: it pins an on-heap segment only *for the duration of the call*, but io_uring's DMA happens **asynchronously after `io_uring_enter` returns**. Data buffers must be **native/direct** (§12) regardless.
- **`critical` + `captureCallState` compose** — allowed at link time on 25.0.3 *(verified; older builds rejected it)*. `captureCallState` adds a small extra store of errno; it does not defeat `critical`, but on a truly hot trivial path you'd drop errno capture and rely on the negative return value.
- **Avoid per-call allocation:** allocate the `io_uring_params`, the errno-capture segment, and any scratch **once** at ring setup in the ring's confined arena and reuse them. SQE/CQE access is into the persistent mmap'd rings — no allocation. Always `invokeExact` with exactly-matching static types (a mismatch throws `WrongMethodTypeException` and, if you fall back to `invoke`, silently boxes).

---

## 12. Direct buffer vs MemorySegment for I/O buffers; address stability & reachability

- **Both direct `ByteBuffer` and native `MemorySegment` (from `arena.allocate`) have GC-stable addresses** — off-heap, never relocated. Either is safe to hand to the kernel. **Heap segments/arrays must never be used** for io_uring data buffers: their address is not stable and the async DMA would corrupt the heap.
- Get the address for the SQE via `MemorySegment.ofBuffer(directBuffer).address()` or `nativeSegment.address()`. `ofBuffer` covers `[position, limit)`, so slice/position the buffer to the exact target range first.
- **Reachability is the real correctness hazard**, because the kernel writes the buffer *after* `io_uring_enter` returns and possibly on another thread. The `PendingRead` entry in the `LongObjectHashMap<PendingRead>` in-flight map (keyed by `user_data`) **must hold a strong reference to the buffer** until the matching CQE is reaped — that map *is* your keep-alive. As a belt-and-braces guard against the JIT treating a local as dead, add `Reference.reachabilityFence(buffer)` at the point you've confirmed the CQE and copied/handed off the data:
  ```java
  // completion side, after reading res from the CQE for this user_data:
  PendingRead p = inflight.remove(userData);
  try { p.future.complete(p.res); }
  finally { Reference.reachabilityFence(p.buffer); }   // buffer stays live until here
  ```
- For O_DIRECT (per the plan's §3.4), address **and** offset **and** length must be block-aligned (512/4096); use `BufferUtil.allocateDirectAligned(...)` since the 2 KiB `BufferPool` unit is only 512-aligned. `MemorySegment.ofBuffer(alignedBuffer).address()` then satisfies the address constraint.
- **`IORING_REGISTER_BUFFERS` + `IORING_OP_READ_FIXED`** pins pages once in the kernel (charged to `RLIMIT_MEMLOCK`), so the SQE carries a `buf_index` instead of an address — removing per-I/O pinning and making the reachability window explicit (the buffer set is pinned for the ring's lifetime, unregistered on teardown). Fall back to unregistered I/O on `ENOMEM`.

---

## Consolidated verified-facts table

| Concern | Verified fact (JDK 25.0.3 / this box) |
|---|---|
| Ring head/tail ordering | segment/value `VarHandle` supports `getAcquire`/`setRelease`/`getVolatile`/CAS/`getAndAdd` — use `getAcquire`/`setRelease` |
| Standalone int handle | `JAVA_INT.varHandle()` → coords `[MemorySegment, long]`; acquire/release supported |
| Alignment | aligned accessor throws `IllegalArgumentException` on unaligned offset; `JAVA_INT_UNALIGNED` for off-boundary |
| Native access default | `warn` (runs + one-time warning, module then marked enabled); `--illegal-native-access=deny` → `IllegalCallerException` |
| Flag needed | `--enable-native-access=ALL-UNNAMED` (server + client + test JVM args) |
| errno | `captureStateLayout()` has member `"errno"`; capture segment is the **leading** handle arg; `close(-1)`→errno 9 |
| `critical`+`captureCallState` | compose OK at link time |
| syscall/mmap/munmap | all present in `defaultLookup()` |
| io_uring syscall nrs | 425/426/427, shared by aarch64 + x86-64 |
| size_t/long | 8 bytes (`canonicalLayouts()` → `j8`) |
| `structLayout` padding | not auto-inserted — add `paddingLayout(n)` to match C ABI |

Probe programs used to verify all of the above live in `/tmp/Sysprobe.java`, `/tmp/Probe2.java`, `/tmp/Probe3.java`, `/tmp/FfmProbe.java` (throwaway; not part of the tree). The existing plan file is `/home/cscotta/projects/cassandra/IO_URING_DISK_ACCESS_MODE_PLAN.md`; JVM options to edit are `/home/cscotta/projects/cassandra/conf/jvm25-server.options` and `/home/cscotta/projects/cassandra/conf/jvm25-clients.options`.