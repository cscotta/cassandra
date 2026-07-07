# io_uring read path — operator & user guide

This guide covers the Linux `io_uring` read path added to Apache Cassandra (branch `cscotta/jdk25`): what it does,
how to turn it on, what it requires, and the caveats to know before relying on it. It is written for operators and
developers evaluating or running the feature.

> **Status.** The feature is functional through the plan's phase P3 and validated end-to-end (unit, deterministic
> fault-injection, disk-access-mode integration, compressed SSTables, and a live-node in-JVM dtest). It is **off by
> default** — you opt in per node via `disk_access_mode`. Two further phases (P4 continuation-passing hot path, P5
> cache-miss-adaptive mode) are pre-designed but intentionally deferred pending measurements.

---

## 1. What it is

`io_uring` is a Linux kernel interface for asynchronous I/O. This feature issues SSTable **data-file reads** through
`io_uring` instead of `FileChannel`, using the JDK 25 Foreign Function & Memory (FFM) API to call the raw `io_uring`
syscalls directly — **no JNI, no `liburing`, no native `.so`**.

What it changes and what it does not:

- **Data-file reads** (uncompressed and compressed SSTables) can go through `io_uring`.
- **Index reads stay on `mmap`** (the resolved index access mode is `mmap` whenever the data read mode is `io_uring`).
- **Writes, commitlog, flushes, and streaming are unchanged.**
- Reads that hit the in-process chunk cache never touch the disk (or `io_uring`) at all — this only affects
  **cache-miss** reads that actually go to the device.

### Where the benefit is (and isn't)

`io_uring`'s advantage is **device-bound reads at high concurrency** — many in-flight reads against the storage device,
e.g. cache-miss point reads under load, compaction, and large scans. Microbenchmarks on this branch showed io_uring
winning **cold/O_DIRECT random reads by up to ~6× at high queue depth** and losing on page-cache-warm reads and on
large low-concurrency sequential reads.

It is **not** faster for reads served from the OS page cache or the chunk cache, and it does not speed up a single
read in isolation. If your working set fits in cache, expect no improvement.

---

## 2. Requirements

| Requirement | Detail |
|---|---|
| **OS** | Linux only. On any non-Linux OS the feature reports unavailable and falls back. |
| **Kernel** | `io_uring` support (kernel ≥ 5.6 for the baseline op set). Newer kernels are recommended. |
| **JDK** | **JDK 25** (the FFM API and this code are compiled only on JDK 25; on JDK 11/17/21 the binding is absent and the feature is unavailable). |
| **JVM flag** | `--enable-native-access=ALL-UNNAMED` — already present in `conf/jvm25-server.options`. Without it, FFM downcalls are denied and the feature reports unavailable. |
| **Kernel sysctl** | `kernel.io_uring_disabled` must not be `2` (system-wide off). Value `1` (restricted) works only for a sufficiently privileged process (see Caveats). |
| **Sandboxing** | Containers/seccomp may block the `io_uring_*` syscalls (see Caveats). |

If any requirement is unmet the node **falls back to `standard`** (buffered `FileChannel`) by default — see §5.

---

## 3. How to enable it

### Point-read / general data path

In `cassandra.yaml`:

```yaml
disk_access_mode: io_uring
```

This routes uncompressed and compressed data-file reads through io_uring; index access is forced to `mmap`.

### Compaction / scan reads (the clearest beneficiary)

Compaction and large scans are the most device-bound read workload, so they benefit most — especially combined with
`O_DIRECT`:

```yaml
compaction_read_disk_access_mode: io_uring
io_uring_direct_io: true          # open the data files O_DIRECT (bypass the page cache) for the device-bound win
```

`compaction_read_disk_access_mode` defaults to `auto` (inherit from `disk_access_mode`); set it explicitly to enable
io_uring for compaction independently of the point-read path.

### Tuning knobs (`cassandra.yaml`)

All are ignored unless io_uring is the resolved read mode.

| Setting | Default | Meaning |
|---|---:|---|
| `io_uring_queue_depth` | `256` | SQ/CQ entries per ring (max in-flight ops); the kernel rounds up to a power of two. |
| `io_uring_poller_threads` | `1` | Number of rings / dedicated completion-poller threads. |
| `io_uring_sqpoll` | `false` | Kernel-side submission polling — burns a core; opt-in for saturated-I/O nodes. (Currently logged and treated as the default submit path.) |
| `io_uring_direct_io` | `false` | Open data files `O_DIRECT` (bypass the page cache). Requires block-aligned reads (satisfied by the chunk path). |
| `io_uring_fallback_on_unavailable` | `true` | If io_uring is unavailable at startup, fall back to `standard` instead of failing to boot. |

### System properties (advanced / experimental)

| Property | Default | Meaning |
|---|---:|---|
| `cassandra.io_uring.read_fixed` | `false` | Use `IORING_OP_READ_FIXED` with pre-registered chunk-cache buffers. **Off by default** — benchmarking showed no advantage over plain `READ` for page-cache reads, and it consumes `RLIMIT_MEMLOCK`. Leave off unless you are measuring it on device-bound O_DIRECT workloads. |
| `cassandra.io_uring.fixed_slab_slots` | `1024` | Max registered-buffer slots when `read_fixed` is on. |

---

## 4. Verifying it is active

**Startup log.** When io_uring resolves successfully you get, at INFO:

```
disk_access_mode=io_uring is available and will be used for the data read path.
```

If it fell back you get a WARN naming the reason (e.g. `kernel.io_uring_disabled=2 ...`, or the probe error).

**JMX metrics.** Metrics are published under MBean type `IoUring`
(`org.apache.cassandra.metrics:type=IoUring,name=<Metric>`):

| Metric | Type | Meaning |
|---|---|---|
| `Submissions` | Meter | Read ops submitted to the ring. **A non-zero, growing count is the definitive proof reads are flowing through io_uring.** |
| `Completions` | Meter | Reaped completions. |
| `CompletionLatency` | Timer | Submit→completion latency (nanoseconds). |
| `Inflight` | Gauge | Ops submitted but not yet completed. |
| `Fallbacks` / `Overflows` | Meter | Reserved; not yet wired to fire (see Caveats). |

---

## 5. Fallback behavior

The read path is designed so that **io_uring being unavailable is a normal, safe condition** (it is unavailable in
many environments). At startup, `checkIoUringAvailability`:

1. Reads `/proc/sys/kernel/io_uring_disabled`; if `2`, reports unavailable without attempting the syscall.
2. Otherwise probes io_uring (a real ring setup + a `NOP` op).

On unavailable, with `io_uring_fallback_on_unavailable: true` (the default) the node **resets `disk_access_mode` to
`standard`** (and `compaction_read_disk_access_mode` likewise, and index access to `mmap`) and logs a WARN — it does
**not** fail to start. Set `io_uring_fallback_on_unavailable: false` to make an unavailable io_uring a hard startup
error instead.

The fallback is also defensive at the reader layer: even if the startup check is skipped (e.g. in tools), a data file
in io_uring mode uses the io_uring reader only when the provider is actually available, else the standard reader.

---

## 6. Caveats & notes

- **It only helps device-bound reads.** No benefit (and slight overhead) for page-cache-warm or chunk-cache-hit reads,
  or large low-concurrency sequential reads. Enable it where reads miss cache and hit the device under concurrency.
- **Availability is environment-sensitive.** Common blockers: non-Linux, JDK < 25, missing
  `--enable-native-access`, `kernel.io_uring_disabled` set, and container/seccomp policies (Docker's default seccomp
  profile blocks the `io_uring_*` syscalls; hardened images may `SCMP_ACT_KILL`). Treat the fallback path as the norm
  and confirm activation via the startup log / `Submissions` metric.
- **`kernel.io_uring_disabled=1` (restricted)** grants io_uring only to `CAP_SYS_ADMIN` or members of
  `kernel.io_uring_group`. The startup check defers this case to the actual syscall probe, so an unprivileged daemon
  will fall back and a suitably privileged one will use io_uring.
- **`O_DIRECT` (`io_uring_direct_io: true`) does not work on `tmpfs`** and requires the data directories to be on a
  filesystem that supports it. Reads are 4 KiB-aligned; the chunk path satisfies this.
- **`RLIMIT_MEMLOCK`** bounds registered buffers. `READ_FIXED` (off by default) is pre-checked against the soft limit
  and falls back to unregistered reads on `ENOMEM`/`EPERM`.
- **Not covered by the deterministic simulator.** Completions arrive off the simulator's scheduler; correctness
  assurance for this path leans on the fault-injection tests and the in-JVM dtest rather than simulation.
- **`Fallbacks`/`Overflows` metrics currently read zero** — the hooks exist but are not yet fired (CQ overflow is
  structurally prevented by sizing the CQ at twice the SQ plus backpressure, so it should not occur in practice).
- **Compressed-SSTable scans** use per-chunk io_uring reads (no read-ahead buffering); this is intentional, since
  io_uring's benefit comes from concurrency, not from larger single reads.
- **Shutdown.** The ring pool is drained and closed during `nodetool drain` / node stop; poller threads are daemon
  threads and do not block JVM exit.

---

## 7. Reverting

Set `disk_access_mode` back to `standard` (or `mmap`), and `compaction_read_disk_access_mode` back to `auto`/`direct`,
then restart the node. This fully disables the io_uring read path; no data format or on-disk change is involved, so it
is safe to toggle per node and to run a mixed cluster during evaluation.

---

## 8. Related documents

- `IO_URING_FFM_LIBRARY_PLAN.md` — the authoritative design & phased delivery plan (§16 roadmap, §15 test plan).
- `IO_URING_DISK_ACCESS_MODE_PLAN.md` — the disk-access-mode integration draft.
