I have everything needed. Here is the complete playbook.

---

# io_uring FFM library — Cassandra build/test/packaging playbook

Scope: a new self-contained package `org.apache.cassandra.io.uring` (later extractable to `modules/io-uring` or a standalone artifact), built/tested/shipped/guarded on the `cscotta/jdk25` branch. All line numbers verified against the working tree.

## 0. Two premise corrections (verified)

- **`--enable-native-access=ALL-UNNAMED` is already present** at `conf/jvm25-server.options:115` (added during the JDK25 work). So the **production server runtime is already covered** (`bin/cassandra.in.sh:144-145` selects `jvm25-server.options` for JDK≥25). It is *not* present in `build.xml` anywhere (`grep` confirms) — so the test/microbench/ant JVMs still lack it.
- **The dominant, non-obvious constraint (empirically proven below): the FFM package can only be compiled on JDK 25.** `java.lang.foreign` is a *preview* API on JDK 21 and does not exist as `java.lang.foreign` on 17. Cassandra compiles with `-source/-target ${ant.java.version}` and **no `--release`**, and CI compiles on **11, 17, 21, and 25**. This forces a ServiceLoader/reflection SPI boundary (see §3, §6) — it is mandatory, not stylistic.

Empirical proof (ran on this machine, `/usr/lib/jvm/java-{21-temurin,25-openjdk}`):
- `javac -source 21 -target 21 FfmProbe.java` → `error: Linker is a preview API and is disabled by default` (Arena, MemorySegment likewise).
- `javac -source 25 -target 25 FfmProbe.java` → compiles clean, no `--enable-preview`.
- Runtime on OpenJDK **25.0.3**: downcall from the classpath **works with only a WARNING** by default (exit 0); `--enable-native-access=ALL-UNNAMED` suppresses the warning; `--illegal-native-access=deny` makes it a fatal `IllegalCallerException` (exit 1).

**Answer to "do FFM downcalls hard-require `--enable-native-access` on JDK 25?": No.** On 25.0.3 they succeed with a warning. The flag suppresses the warning and is forward-compatible ("Restricted methods will be blocked in a future release"). Add it anyway, and add a CI cell with `--illegal-native-access=deny` to guarantee the wiring never rots.

---

## 1. build.xml — compile model, JDK-25 wiring, where to add the flag

**How `src/java` compiles** — `build.xml:778-793` (`_build_java`):
- `<javac source="${ant.java.version}" target="${ant.java.version}">` (line 784). Explicit comment at 779-780: *"we cannot use javac's 'release' option"* — so the visible API is the **compiling JDK's**, and the same source must compile on 11/17/21/25.
- Annotation processing: `${javac.annotation.processing}` (applied line 789 main, 1340 test) = `-proc:full` computed at **`build.xml:538-545`**, guarded OFF for 11/17 (JDK 23+ disabled implicit processing, JDK-8321319; needed for `compile-command-annotations` → `META-INF/hotspot_compiler` and, in the test compile, `jmh-generator-annprocess`).
- `${jdk11plus-javac-exports}` (line 532) supplies the `--add-exports` needed to *compile* against `sun.nio.ch`/`jdk.internal.ref` — already includes `--add-exports java.base/sun.nio.ch=ALL-UNNAMED` and `java.base/jdk.internal.ref=ALL-UNNAMED`, which the fd-extraction code needs.

**JDK-25 option files wired:**
- Runtime: `bin/cassandra.in.sh:144-145` → `conf/jvm25-{server,clients}.options`.
- Build/test JVM args: `_jvm25_arg_items` (`build.xml:339-384`) → property `java-jvmargs` (set at 386-388 when `ant.java.version==25`); `_jvm25_test_arg_items` (`build.xml:501-513`) → `_std-test-jvmargs` (set at 525-527).

**Where to add `--enable-native-access=ALL-UNNAMED`:**
- **(a) server runtime** — already at `conf/jvm25-server.options:115`. (Leave `jvm25-clients.options` alone unless a client tool touches io_uring; it doesn't.)
- **(b) unit-test JVM** — the test JVM assembles `${java-jvmargs}` (`build.xml:1496`) **and** `${_std-test-jvmargs}` (`build.xml:1499`); neither has the flag. **Best single edit: add one line to `_jvm25_arg_items` just before `</resources>` at `build.xml:383`:**
  ```xml
  <string>--enable-native-access=ALL-UNNAMED</string>
  ```
  Because `java-jvmargs` is consumed by the unit tests (1496), `check-test-names` (1383), and the JMH runner (below), this one edit covers (b) and (c). (If you prefer to scope it to tests only, add it to `_jvm25_test_arg_items` at ~line 508 instead — but then `check-test-names`, which loads the bench classes, won't have it.)
- **(c) JMH/microbench runner** — `.build/build-bench.xml:101-102` uses `${java-jvmargs}` + `${_std-test-jvmargs}`; covered by the same edit. JMH **forks** worker JVMs; the harness inherits the parent's input args into forks by default (proven in-tree: existing benches like `ReadWriteBench` rely on `--add-opens`/`--add-exports` without redeclaring them). Belt-and-suspenders: also annotate the bench `@Fork(jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED"})`.

**Compile-exclusion edit (the load-bearing one).** In `_build_java` (`build.xml:782`) and `_build-test` (`build.xml:1321`) add a conditional `excludes`, e.g.:
```xml
<!-- near the other conditions, ~line 545 -->
<condition property="ffm.src.excludes" value="" else="org/apache/cassandra/io/uring/**,**/microbench/*IoUring*.java,**/io/uring/*Test.java">
    <equals arg1="${ant.java.version}" arg2="25"/>
</condition>
```
then `excludes="${ffm.src.excludes}"` on both `<javac>` tasks. On 11/17/21 the FFM sources vanish from the compile; on 25 they build.

---

## 2. Test layout + copy-paste recipes

**Directories** (`build.xml:81-89`): `test/unit` (`test.unit.src`), `test/distributed`, `test/microbench` (package **`org.apache.cassandra.test.microbench`**), `test/long`, `test/burn`, `test/memory`, `test/simulator/{main,test,asm,bootstrap}`, `test/harry`. All compiled by one javac in `_build-test` (`build.xml:1320-1354`).

**Running a unit test:** `ant testsome -Dtest.name=<FQCN> [-Dtest.methods=<m>]` (target `build.xml:1690`). The per-test JVM is assembled in `testmacrohelper` → `junit-timeout` (`build.xml:1458-1528`): fixed args 1461-1495, then `${java-jvmargs}` (1496), `${_std-test-jvmargs}` (1499), `${test.jvm.args}` (1500). Whole-class alternative: `ant test -Dtest.name=<simple-class>`.

**JMH microbench** — `.build/build-bench.xml`: targets `microbench` (49), `microbench-test` (53, one-shot smoke), `microbench-with-profiler` (66, async-profiler). Runner is `org.openjdk.jmh.Main` (92). Run a single bench: `ant microbench -Dbenchmark.name=IoUring` → filter `.*microbench.*IoUring` (build-bench.xml:127). Pass JMH flags via `-Djmh.args="-f 1 -wi 3 -i 5 -t 8"` (119). Two hard conventions: benches must be in a package whose FQN **contains `microbench`** (the run filter, 127) and the class name must **end in `Bench`** (`check-test-names` regex, `build.xml:1397`). The JMH harness classes are generated by `jmh-generator-annprocess` during the test compile (needs `-proc:full`, already wired 1338-1340). Note the broken-bench blacklist `microbench.exclude.pattern` (build-bench.xml:26) — only applied when `benchmark.name` is blank, so a named run is never blacklisted.

**Recipe — unit test:** `test/unit/org/apache/cassandra/io/uring/IoUringBindingTest.java`
```java
/* <ASF header — see §5> */
package org.apache.cassandra.io.uring;

import org.junit.Test;
import org.apache.cassandra.utils.FBUtilities;
import static org.junit.Assume.assumeTrue;
import static org.junit.Assert.assertTrue;

public class IoUringBindingTest
{
    @Test
    public void setupEnterExitRoundTrip()
    {
        assumeTrue("Linux only", FBUtilities.isLinux);            // FBUtilities.java:128
        assumeTrue("JDK 25+ only", Runtime.version().feature() >= 25);
        assumeTrue("io_uring unavailable", IoUringAvailability.check().isAvailable());
        assertTrue(/* ring setup/enter/reap round-trip */ true);
    }
}
```
Run: `ant testsome -Dtest.name=org.apache.cassandra.io.uring.IoUringBindingTest`

**Recipe — JMH bench:** `test/microbench/org/apache/cassandra/test/microbench/IoUringChunkReaderBench.java`
```java
/* <ASF header> */
package org.apache.cassandra.test.microbench;   // must contain "microbench"

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1) @Measurement(iterations = 5, time = 2)
@Fork(value = 1, jvmArgsAppend = {"--enable-native-access=ALL-UNNAMED"})
@State(Scope.Benchmark)
public class IoUringChunkReaderBench          // must end in "Bench"
{
    @Setup public void setup() { /* org.junit.Assume not available here — guard by falling back / skipping in @Setup */ }
    @Benchmark public int readOneChunk() { return 0; }
}
```
Run one: `ant microbench -Dbenchmark.name=IoUringChunkReader`  ·  Smoke all: `ant microbench-test -Dbenchmark.name=IoUring`  ·  Profile: `ant microbench-with-profiler -Dbenchmark.name=IoUring`

---

## 3. Extractability — dependency hygiene & the modules/ model

**Deps to keep vs. hide behind an SPI (so extraction is a move, not a rewrite):**

| Concern | In-tree reality | Extraction verdict |
|---|---|---|
| Logging | slf4j (`org.slf4j`) is used everywhere | **Keep** — slf4j is a normal external dep, extractable as-is. |
| Config | `Config.DiskAccessMode` enum (`Config.java:1355`), `DatabaseDescriptor`, and **`CassandraRelevantProperties`** (checkstyle bans raw `System.getProperty`, §5) | **SPI.** Define a small immutable config value object *inside* `io.uring` (queueDepth, pollerThreads, sqpoll, fallback, blockSize); core populates it. Never read `System.*` or `DatabaseDescriptor` from inside the package. |
| Buffer pool | `BufferPool` uses `FastThreadLocal` + `jdk.internal.ref` | **SPI** `BufferAllocator { allocateAligned(len,align); free(buf); }`. Default impl delegates to `BufferPool`; a standalone default can use `ByteBuffer.allocateDirect`. |
| Threads | `executorFactory().infiniteLoop(...)` (`BufferPool.java:199`); checkstyle `blockExecutors` bans raw pools (§5) | **SPI** — take a `ThreadFactory`/poller-runner. Core wires `ExecutorFactory`; standalone uses a plain factory. |
| Metrics | JMX `ThreadPoolMetrics` | **SPI/no-op** callback interface. |
| Futures/locks | checkstyle **bans** `java.util.concurrent.{CompletableFuture,Semaphore,CountDownLatch}` and Netty futures (§5); tree substitutes `org.apache.cassandra.utils.concurrent.{Future,Promise,AsyncPromise,Semaphore,CountDownLatch}` | Either use Cassandra's `utils.concurrent.*` (adds a core dep) **or** use plain JDK types with `// checkstyle: permit this import` per line. For clean extraction, expose a minimal `Future`-like handle at the SPI boundary and permit JDK `CompletableFuture` internally. |
| File/fd | checkstyle bans `java.io.File`/`RandomAccessFile`; fd extraction via `NativeLibrary.getfd` / `ChannelProxy.getFileDescriptor()` (`NativeLibrary.java:379`) | **SPI takes a raw `int fd`** (+ optional `FileChannel`). No `File` dependency inside the lib at all. |

There is **no near-standalone, few-deps example** in `src/` (io.util readers are coupled). The correct model is the **accord submodule**:
- `.gitmodules` → `modules/accord` (submodule of `github.com/apache/cassandra-accord`, branch `trunk`). `build.xml:115` `accord.dir=modules/accord`.
- Built by its **own Gradle** via `.build/build-accord.xml` `_build-accord` (`gradlew clean build publishToMavenLocal`, lines 22-44), invoked from `_build_subprojects` (`build.xml:774`, part of `build` at 775). Output `cassandra-accord-*.jar` is consumed as a resolver dependency.
- accord's own rat/checkstyle are **disabled** in its Gradle (`build-accord.xml:30-36`) because Cassandra re-runs them over accord source; rat inclusion is via `.build/rat-include-accord.sh` (lists the submodule's files, since `git ls-tree HEAD` of the parent only sees the pointer — see §5).
- Guard: accord builds are skippable with `-Dno-build-accord` (`build-accord.xml:21`).

**Extraction path (recommended):** Phase 0 — keep source in `src/java/.../io/uring`, compiled only on JDK 25 (§1 exclude), all core coupling through the SPIs above + ServiceLoader (§6). Phase X — `git submodule add` a `modules/io-uring` repo, mirror `build-accord.xml` as `.build/build-io-uring.xml` (its Gradle **pins source/target to 25**, unlike accord's 11, and the whole module build is skipped when the outer JDK<25, analogous to `-Dno-build-accord`), add `rat-include-io-uring.sh`, and consume the jar via the resolver. The SPI boundary makes this a mechanical move.

---

## 4. Native binding: pure-syscall FFM vs. a C shim

**There is no native/C build in the tree today.** `find` shows only `doc/Makefile` (docs) and a *downloaded* `libasyncProfiler.so` (build-bench.xml:76). Existing native access is via **JNA/JNR** (`NativeLibraryLinux`/`NativeLibraryDarwin`, jnr-ffi 2.2.13, jna 5.13.0) — **no `System.loadLibrary` of any Cassandra-built `.so`**, no `cc`/`gcc` ant target, no per-arch `.so` packaging. FFM (`java.lang.foreign`) appears **nowhere** in `src/` — this is the first.

**If the C shim `libcassandra_uring.so` were the fallback**, you would have to add entirely new build+release machinery that does not exist:
- a per-arch (`x86-64`, `aarch64`) `<exec executable="cc">` compile target,
- embed the `.so` in the jar under an arch-specific resource path,
- extract-to-temp + `System.load()` at runtime (the netty-tcnative / snappy-java / lz4-java pattern),
- plus ASF release-signing/provenance for binaries.

**Comparison — bind libc `syscall(2)` directly (plan Option A):** needs **zero** native artifact — one FFM downcall handle to `syscall` with the per-arch io_uring numbers (`425/426/427`), `Linker.Option.captureCallState("errno")`, `Linker.Option.firstVariadicArg(...)`, plus `mmap`/`munmap` handles. **Recommendation: pure-syscall FFM, no shim.** The shim only buys named symbols and easier auditing, at the cost of a whole native toolchain + per-arch packaging + binary release process the project has never had.

---

## 5. Quality gates a new package must pass

**The only static gates are checkstyle + rat.** Verified: no forbidden-apis plugin, no import-control, no signatures file anywhere; `eclipse-warnings` is a **no-op stub** (`build.xml:2381-2383`, guarded `if="java.version.8"`); CheckerFramework is only referenced in a comment, not wired. `ant check` (`build.xml:808-812`) = `rat-check` + `checkstyle` + `checkstyle-test`.

**Checkstyle** (`.build/build-checkstyle.xml`; run directly: `ant checkstyle checkstyle-test` — note the memory gotcha that `.build/sh/ai-build` silently skips it). Config `.build/checkstyle.xml` (main, over `${build.src.java}`), `.build/checkstyle_test.xml` (tests), suppressions `.build/checkstyle_suppressions.xml` (empty). Rules that touch an FFM/`sun.nio.ch` package:
- **No ban on `java.lang.foreign.*` or `sun.nio.ch.*`.** `IllegalImport` (`checkstyle.xml:102-105`) bans `illegalPkgs=junit.framework,org.jboss.byteman` and a specific `illegalClasses` list — FFM and `sun.nio.ch` are absent → **clean**.
- `blockSystemPropertyUsage` (179-184): bans `System.getProperty/setProperty/getenv`, `Integer.getInteger`, etc. → use `CassandraRelevantProperties`, or `// checkstyle: suppress nearby 'blockSystemPropertyUsage'`. (Reinforces the config SPI in §3.)
- `IllegalImport` illegalClasses (104): bans `java.util.concurrent.{Semaphore,CountDownLatch,Executors,CompletableFuture,...}`, `java.io.{File,RandomAccessFile,...}`, `java.nio.file.Paths` → the plan's `CompletableFuture`/`Semaphore` usage fails unless you use `org.apache.cassandra.utils.concurrent.*` or add `// checkstyle: permit this import` (SuppressWithNearbyCommentFilter, 42-46).
- `blockExecutors` (88-94) → `ExecutorFactory`; `blockGuavaDirectExecutor` (95-101); `blockToCases` (160-166) `toLowerCase/toUpperCase` → `LocalizeString`; `IllegalType var` (186-188) → the **`var` keyword is banned**.
- `ImportOrder` (191-197): groups `java, javax, com, net, org, accord, org.apache.cassandra` (last), separated, static bottom, sorted. `AvoidStarImport` (190), `UnusedImports`/`RedundantImport` (199-200), `MissingDeprecated` + `@Deprecated` must carry `since` (203-208).

**rat / license header** (`.build/build-rat.xml`, target `rat-check`, part of `ant check`). It audits **git-tracked files** (`git ls-tree -r HEAD`, line 30) plus accord via `.build/rat-include-accord.sh`; fails if `rat.txt` isn't "0 Unknown Licenses" (112-120). No `.java` exclusion → **every new `.java` needs the exact ASF header** (verified copy):
```
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
```
Caveat: a `META-INF/services/...` ServiceLoader file (needed for the SPI in §6) **cannot carry a comment header** — add a `<exclude>` for it in `build-rat.xml` (like the existing config-file excludes at lines 45-107). Same for any future `.c` shim (it *can* take the header) or generated files.

---

## 6. CI matrix + self-skip strategy

- **GitHub Actions** `.github/workflows/code-check.yaml`: runs `ant check` (compiles `_main-jar` + `build-test`, then rat+checkstyle) on **JDK 11 and JDK 17** via `.build/docker/check-code.sh {11,17}`.
- **CircleCI** `.circleci/config.yml`: env pins **JDK 11 and JDK 17** (`java-11-openjdk-amd64`, `java-17-openjdk-amd64`).
- **Jenkins** `.jenkins/Jenkinsfile:241-256`: matrix axis `jdk = java.supported` = **11,17,21,25** × arch × step — builds and tests on **all four** JDKs.

**Implication:** because the FFM package won't compile on 11/17 (missing API) or 21 (preview, proven in §0), it must be **compile-excluded on every JDK except 25** (the `${ffm.src.excludes}` edit in §1). Consequently **core code cannot statically reference the io_uring classes** on the 11/17/21 cells — hence a **ServiceLoader boundary is mandatory**:

1. A stable SPI interface in core (compiled on all JDKs), e.g. `org.apache.cassandra.io.util.AsyncReadProvider` (or the `ChunkReader` factory seam the plan already targets).
2. The FFM impl in `org.apache.cassandra.io.uring`, registered via `src/resources/META-INF/services/...AsyncReadProvider` — **present in the jar only on the 25 build**.
3. Core does `ServiceLoader.load(...)`; on <25 builds it finds nothing → falls back to `standard` (matches the plan's `io_uring_fallback_on_unavailable`).

**Self-skip so 11/17/21 and macOS stay green:**
- Since the tests/benches are compile-excluded on <25, they simply don't exist in those cells → green with no runtime logic.
- On any JDK-25 cell (incl. macOS if present), each test still guards at runtime:
  ```java
  assumeTrue(FBUtilities.isLinux);                       // FBUtilities.java:128
  assumeTrue(Runtime.version().feature() >= 25);
  assumeTrue(IoUringAvailability.check().isAvailable()); // kernel ≥5.6 + io_uring_setup probe; ENOSYS/EPERM → skip
  ```
- Add a Jenkins/local CI cell that runs the io_uring tests with **`--illegal-native-access=deny`** to prove `--enable-native-access` stays wired (empirically fatal without it, §0).
- Keep io_uring out of the simulator (per the existing plan §7.6) — the simulator step already runs default-JDK-only in Jenkins (`Jenkinsfile:261`).

---

## Quick command reference

```
# Build on JDK 25 (FFM package compiles only here)
JAVA_HOME=/usr/lib/jvm/java-25-openjdk ant realclean build build-test

# Static gates (ai-build skips checkstyle — run directly)
ant rat-check checkstyle checkstyle-test

# Unit test
ant testsome -Dtest.name=org.apache.cassandra.io.uring.IoUringBindingTest

# Single JMH bench (+ prove the flag with deny)
ant microbench -Dbenchmark.name=IoUringChunkReader -Djmh.args="-f 1 -wi 3 -i 5 -t 8"
ant microbench-with-profiler -Dbenchmark.name=IoUring

# Reproduce the JDK-21-preview / JDK-25-final and native-access findings
javac -source 21 -target 21 FfmProbe.java          # fails: preview API disabled
javac -source 25 -target 25 FfmProbe.java           # ok
java --illegal-native-access=deny FfmProbe          # IllegalCallerException without --enable-native-access
```

## Exact edits, summarized
1. `build.xml:383` — add `<string>--enable-native-access=ALL-UNNAMED</string>` to `_jvm25_arg_items` (covers unit tests, `check-test-names`, JMH).
2. `build.xml` ~545 — add `ffm.src.excludes` condition; apply `excludes="${ffm.src.excludes}"` to `_build_java` javac (782) and `_build-test` javac (1321).
3. New `src/resources/META-INF/services/<SPI>` + a matching `<exclude>` in `.build/build-rat.xml`.
4. Every new `.java` — ASF header (§5); package `org.apache.cassandra.io.uring` (src) / `org.apache.cassandra.test.microbench` (benches); no `var`, no banned `java.util.concurrent`/`java.io.File`/`System.getProperty` imports.
5. Server runtime flag — already present (`conf/jvm25-server.options:115`), no edit.

Relevant files: `/home/cscotta/projects/cassandra/build.xml`, `/home/cscotta/projects/cassandra/.build/build-bench.xml`, `/home/cscotta/projects/cassandra/.build/build-checkstyle.xml`, `/home/cscotta/projects/cassandra/.build/checkstyle.xml`, `/home/cscotta/projects/cassandra/.build/build-rat.xml`, `/home/cscotta/projects/cassandra/.build/build-accord.xml`, `/home/cscotta/projects/cassandra/.build/rat-include-accord.sh`, `/home/cscotta/projects/cassandra/conf/jvm25-server.options`, `/home/cscotta/projects/cassandra/bin/cassandra.in.sh`, `/home/cscotta/projects/cassandra/.github/workflows/code-check.yaml`, `/home/cscotta/projects/cassandra/.jenkins/Jenkinsfile`, `/home/cscotta/projects/cassandra/.circleci/config.yml`, `/home/cscotta/projects/cassandra/.gitmodules`.