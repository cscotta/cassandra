# io_uring research corpus

Raw research reports underpinning [`../IO_URING_FFM_LIBRARY_PLAN.md`](../IO_URING_FFM_LIBRARY_PLAN.md). Preserved verbatim so the plan's claims are auditable and the research survives context clears.

**Provenance:** produced 2026-07-05 by a parallel multi-agent research fan-out (7 investigations + 1 synthesis) on branch `cscotta/jdk25`. The ABI, FFM, and codebase facts were **empirically verified on the dev host** — aarch64, kernel `7.0.13-400.asahi.fc44.aarch64+16k`, OpenJDK `25.0.3`, glibc `2.43`, `liburing 2.13`, header `/usr/include/linux/io_uring.h` (2025-06-23) — by compiling `offsetof`/`sizeof` probes and running FFM probe programs, not by recall. File:line references are current as of commit `52ed036ac8`.

## Contents

| File | What it is | Key content |
|---|---|---|
| [`01-kernel-abi.md`](01-kernel-abi.md) | io_uring kernel ABI reference | Verified struct sizes/offsets (`params`=120B, `sqe`=64B, `cqe`=16B, sq_off@40/cq_off@80); syscalls 425/426/427 (x86-64=aarch64); full opcode/setup/feature/SQE/CQE/register-flag tables with kernel versions; mmap layout + the acquire/release barrier matrix; O_DIRECT alignment. |
| [`02-liburing-api-completeness.md`](02-liburing-api-completeness.md) | liburing → Java API checklist | Every `io_uring_prep_*` / submit / complete / register symbol mapped to a proposed Java method, marked must-have/nice-to-have/omit; the exact internal `get_sqe`/`flush_sq`/`needs_enter` contract to reimplement; the minimum-viable general-purpose API. |
| [`03-cassandra-disk-access-mode.md`](03-cassandra-disk-access-mode.md) | Verified `disk_access_mode` subsystem | The enum, resolution logic, full read call-chain with file:line, `FileHandle`/`ChannelProxy`/`ChunkReader`/`ChunkCache` seams, StartupChecks, cassandra.yaml; the enumerated list of every integration seam a new mode must touch; corrections to the earlier draft. |
| [`04-cassandra-native-infra-reuse-map.md`](04-cassandra-native-infra-reuse-map.md) | Reusable Cassandra infrastructure | fd extraction (`NativeLibrary.getfd`), `BufferPool` geometry/alignment, `MemoryUtil.getAddress`, `ExecutorFactory.infiniteLoop` lifecycle, StartupChecks framework, config plumbing, metrics pattern, env detection — each with file:line + the SPI seam needed for extraction. |
| [`05-jdk25-ffm-binding-reference.md`](05-jdk25-ffm-binding-reference.md) | JDK 25 FFM binding reference | Verified snippets: variadic-`syscall` downcall handles, `captureCallState("errno")`, mmap/munmap, the **acquire/release `VarHandle` answer** for ring head/tail, Arena/thread-confinement choices, buffer reachability, `critical`, `--enable-native-access` measured behavior on 25.0.3. |
| [`06-prior-art-and-safety.md`](06-prior-art-and-safety.md) | Prior art + safety/hazards | Comparison of Netty/nio_uring/jasyncfio/JUring/PanamaUring; io_uring's disable/seccomp/`io_uring_disabled`/gVisor/SIGSYS operational posture; the H1–H15 memory-safety hazard→mitigation table; testing strategy; concurrency-model recommendation; sources. |
| [`07-build-test-packaging.md`](07-build-test-packaging.md) | Build/test/packaging playbook | Exact `build.xml` edits (compile-exclusion, `--enable-native-access`), JDK-25-only compile constraint (empirically proven), JUnit/JMH recipes, checkstyle/rat gates, ServiceLoader boundary, CI matrix, the `accord`-submodule extraction model. |
| [`08-architect-synthesis.md`](08-architect-synthesis.md) | Lead-architect synthesis | Reconciles all seven reports into the layered architecture, API sketch, test plan, phased delivery, extraction plan, open questions, and completeness critique that seed the plan document. |

## How this maps to the plan

The plan (`IO_URING_FFM_LIBRARY_PLAN.md`) is the distilled, decision-resolved output; these files are the primary sources. When the plan cites a struct offset, a file:line, or a hazard number (H1–H15), the authority is here. Report `08` is closest to the plan's structure; `01`/`05` back its ABI/FFM sections; `03`/`04` back the Cassandra-integration sections; `06` backs the safety/availability/concurrency sections; `07` backs build/test.

**Note:** these are research notes, not ASF-licensed source — they carry no license header and should not be committed to the Apache tree as-is. They are a working corpus for the implementer; discard or relocate before any upstream contribution.
