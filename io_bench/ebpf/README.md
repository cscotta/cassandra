# io_bench eBPF collectors

Attribute Apache Cassandra's disk IO on a single node whose data lives on an
**XFS filesystem on a loop device** (e.g. `/dev/loop0`). We measure at the loop
device on purpose: the loop backing file usually sits on btrfs, and tracing the
loop device isolates Cassandra's IO from btrfs bookkeeping noise.

Cassandra's **chunk cache is disabled**, so every logical sstable read issues a
`pread()` to the OS. Whether that `pread` is served from the OS page cache or
turns into a device fetch is exactly what these collectors separate: compare the
VFS layer (`vfs_reads.bt`) to the block layer (`block_io.bt`) over the same
window.

Two headline metrics:

* **IOPS per request** — VFS `pread` count vs device read-completion count.
* **bytes per request** — VFS requested bytes vs device fetched bytes
  (`nr_sector * 512`).

Plus **page-cache hit rate** and **readahead over-fetch**, both derived from the
VFS-vs-block delta (below).

Verified on: aarch64 Asahi Linux, kernel `7.0.13-400.asahi.fc44.aarch64+16k`
(16 KB pages), bpftrace v0.24.2, bcc tools in `/usr/share/bcc/tools`.

---

## Files

| file | what it does |
|------|--------------|
| `probe_check.sh` | preflight: verifies every tracepoint + bcc tool exists and prints the exact field names on this kernel. Run it first. |
| `block_io.bt`    | block-layer collector, filtered to one device (dev_t). Device read IOPS, read bytes, latency + size histograms, per-thread attribution. |
| `vfs_reads.bt`   | VFS-layer collector, filtered to one PID. `pread` count + bytes, per-fd and per-comm breakdown, size histogram. |
| `README.md`      | this file. |

---

## Invocation and the substitution mechanism

Both `.bt` scripts take their filter value as a **bpftrace positional parameter
`$1`** — a plain integer passed on the command line after the script name. This
is native bpftrace and needs no text templating; an unprovided `$1` defaults to
`0`, so `--dry-run` still parses. (No `__DEV__`/sed placeholder and no env-var
read is used; bpftrace v0.24 has no in-script `getenv`.)

### `block_io.bt <DEV_T>` — device filter

`<DEV_T>` is the **kernel `dev_t`** of the loop device, which the block
tracepoints encode as:

```
dev_t = (MAJOR << 20) | MINOR
```

This is **not** the glibc `st_rdev` encoding, so do **not** use `stat -c %t/%T`.
Read the decimal `MAJOR:MINOR` from sysfs and compute it:

```sh
read MAJ MIN < <(tr ':' ' ' < /sys/class/block/loop0/dev)   # e.g. "7 0"
DEV_T=$(( (MAJ << 20) | MIN ))                               # /dev/loop0 -> 7340032
sudo bpftrace io_bench/ebpf/block_io.bt "$DEV_T"
```

(`lsblk -o NAME,MAJ:MIN` prints the same decimal major:minor. The `(major<<20)`
encoding was confirmed on this host: `nvme0n1` = `259:0` showed up in the
tracepoint as `dev=271581184` = `259<<20`.)

### `vfs_reads.bt <PID>` — process filter

`<PID>` is the Cassandra JVM process id (tgid). It is compared against the
bpftrace `pid` builtin (which is the tgid), so **all** JVM threads match:

```sh
sudo bpftrace io_bench/ebpf/vfs_reads.bt "$(pgrep -f 'org.apache.cassandra' | head -1)"
```

### Running both for a window

Start both under a shared timeout, drive the workload, let them print on exit:

```sh
sudo timeout -s INT 60 bpftrace -B line io_bench/ebpf/block_io.bt "$DEV_T" > block.out 2>&1 &
sudo timeout -s INT 60 bpftrace -B line io_bench/ebpf/vfs_reads.bt "$PID"    > vfs.out   2>&1 &
wait
```

Use `-B line` when redirecting to a file so output is flushed as it is produced.
Both scripts print a single `SUMMARY ...` line (the primary parse target),
followed by bpftrace's `@name[key]: value` map dumps.

---

## Output fields

### `block_io.bt`

`SUMMARY` line (counted at `block_rq_complete`):

| key | meaning |
|-----|---------|
| `read_completions`  | device **read IOPS** over the window (read requests completed) |
| `read_bytes`        | device **read bytes** = sum of `nr_sector * 512` for reads |
| `write_completions` | write requests completed |
| `write_bytes`       | write bytes = sum of `nr_sector * 512` for writes |
| `other_completions` | flush-only / discard / other (not read or write) |

Map dumps:

| map | meaning |
|-----|---------|
| `@read_lat_us`             | issue→complete latency histogram (µs), power-of-2 buckets. Keyed by `(dev, sector)` at `block_rq_issue`. |
| `@read_req_bytes`          | per-request size histogram (bytes) for reads |
| `@rd_issue_cnt_by_comm[comm]`   | read **submissions** per issuing thread name |
| `@rd_issue_bytes_by_comm[comm]` | read **bytes submitted** per issuing thread name (`args->bytes` at issue) |
| `@rd_issue_by_thread[comm, pid, tid]` | full issuer identity (thread name + tgid + tid) |

Read vs write is classified from `rwbs[10]`: first char `R` (0x52) = read,
`W` (0x57) or preflush-`FW` = write, else *other*. (Character literals are not
supported by this bpftrace, so ASCII codes are used, and `rwbs` — which bpftrace
surfaces as a string — is cast to `(int8 *)` to read individual bytes.)

**Why per-comm is measured at issue, not complete:** `block_rq_complete` runs in
softirq/IRQ context, where `comm` is a random interrupted task, not the
submitter. The issuing thread name (`ReadStage-N`, `CompactionExecutor-N`) is
only meaningful at `block_rq_issue`, which fires in the submitting thread. So
headline totals come from *complete*; ReadStage-vs-Compaction attribution comes
from *issue*. Their read-byte totals should track closely.

### `vfs_reads.bt`

`SUMMARY` line (counted at syscall entry):

| key | meaning |
|-----|---------|
| `pread_count`   | total `pread64` calls = **VFS read IOPS** |
| `pread_bytes`   | total **requested** bytes (`args->count`, i.e. what was asked, not what was returned) |
| `preadv_count`  | `preadv` calls (see caveat) |
| `preadv2_count` | `preadv2` calls (see caveat) |

Map dumps:

| map | meaning |
|-----|---------|
| `@pread_cnt_by_fd[fd]`   | pread count per file descriptor |
| `@pread_bytes_by_fd[fd]` | requested bytes per fd |
| `@pread_req_bytes`       | requested-size histogram |
| `@pread_cnt_by_comm[comm]` | pread count per JVM thread name |
| `@preadv_cnt_by_fd[fd]` / `@preadv2_cnt_by_fd[fd]` | preadv* calls per fd |

**fd → sstable path:** the orchestrator resolves each `fd` to a path by reading
`/proc/<PID>/fd/<fd>` at parse time (while the process still holds it), mapping
each fd to its sstable component (`-Data.db`, `-Index.db`, ...).

**preadv caveat:** `sys_enter_pread64` carries the byte count directly in
`count`, but `sys_enter_preadv`/`preadv2` do not — the byte count is spread over
`vlen` iovecs in the user-space `vec` array, so these are **call-counted only**.
Cassandra's positioned sstable reads use `pread64` (single `ByteBuffer`), so
`preadv*` are expected to be ~0; a nonzero count flags an unexpected path.

---

## Page-cache hit derivation (VFS vs block)

Over the **same window**, for the target device/PID:

```
page-cache hit bytes   ≈  VFS requested bytes  −  block-device read bytes
                       =  vfs.pread_bytes       −  block.read_bytes

page-cache hit ratio   ≈  (pread_bytes − read_bytes) / pread_bytes
```

Whatever the VFS layer asked for but the block layer did **not** have to fetch
was served from the OS page cache. IOPS-wise, `vfs.pread_count` vs
`block.read_completions` tells the same story per request.

Cross-check with `cachestat` / `cachetop` (below), which report hit ratios
directly from mm counters.

---

## Readahead over-fetch and the 16 KB page floor

`block.read_bytes` can **exceed** `vfs.pread_bytes`. That surplus is
**readahead over-fetch** — the kernel pulled more than was asked. Two floors
inflate it on this host:

* **16 KB base pages.** The page cache fills in whole pages, so any read that
  misses cache pulls at least one **16 KB** page even if the `pread` asked for
  4 KB or less. A cold random 4 KB read therefore costs ≥16 KB of device IO
  before readahead is even considered.
* **Readahead window.** The kernel reads ahead in multiples of the page size up
  to the device's `read_ahead_kb`. Check the loop device's setting before a run:

  ```sh
  cat /sys/block/loop0/queue/read_ahead_kb     # KB per readahead window
  blockdev --getra /dev/loop0                  # same, in 512-byte sectors
  ```

  (For reference, `nvme0n1` on this host defaults to `read_ahead_kb=4096`;
  the loop device gets its own default when created — verify it.)

Define **over-fetch ratio** = `block.read_bytes / vfs.pread_bytes`. A cold,
random, small-read workload with a wide readahead window can show a large ratio;
to see the *pure* page floor, set `read_ahead_kb=0` (or open with the read path
that disables readahead) so the only inflation is the 16 KB page.

---

## Secondary cross-checks (stock bcc tools)

Run these alongside the `.bt` collectors to corroborate; all live in
`/usr/share/bcc/tools`:

| tool | use |
|------|-----|
| `sudo cachetop -p <PID> 1` | live page-cache HITS/MISSES/DIRTIES and hit% per process — the direct page-cache-hit number to compare against the VFS−block derivation. |
| `sudo cachestat 1`         | system-wide page-cache hit ratio per second. |
| `sudo readahead`           | histogram of how much readahead was used vs wasted (pages read ahead but evicted unused) — quantifies over-fetch independently. |
| `sudo biosnoop -d <DEV>`   | per-BIO trace (pid, comm, sector, bytes, latency) filtered to the device — spot-check against `block_io.bt`. `<DEV>` is the `name` (e.g. `loop0`). |
| `sudo biopattern`          | sequential-vs-random ratio and IOPS per device. |
| `sudo biolatency -D`       | block IO latency histogram per device — corroborates `@read_lat_us`. |
| `sudo filetop -p <PID>`    | top files by read/write bytes for the process. |

---

## Validation performed

* `probe_check.sh` → **PASS**: all tracepoints and bcc tools present; field
  names printed match those baked into the `.bt` scripts.
* `sudo bpftrace --dry-run block_io.bt <DEV_T>` and `... vfs_reads.bt <PID>`
  both attach all probes against the live kernel and exit 0 (no field/type
  errors).
* Live functional test, `block_io.bt` on `nvme0n1` while reading the raw device:
  `read_completions` and `read_bytes` matched the `dd` count and size exactly,
  latency + size histograms and per-thread attribution populated.
* Live functional test, `vfs_reads.bt` on a pread generator: `pread_bytes`
  equalled `count × size` exactly, with correct per-fd and per-comm breakdown.

Everything works with what is installed — no `pip`/`dnf` steps required.
