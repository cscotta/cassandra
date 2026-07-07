# Plan: Support JDK 25 in Apache Cassandra

## Context

Cassandra currently declares support for JDK 11, 17, and 21 (`build.xml:48`, `bin/cassandra.in.sh:117`). JDK 25 is the September-2025 LTS release and the next LTS after 21. Between 21 and 25 the JDK removed or fundamentally changed several mechanisms Cassandra depends on — most critically the **Security Manager**, which is permanently disabled in JDK 24 (JEP 486) and is load-bearing in two places in the Cassandra runtime: the UDF sandbox (`ThreadAwareSecurityManager`) and the JMX authorization proxy (`AuthorizationProxy`). The 21-era options files also set flags that are fatal on 25 (`-XX:+ZGenerational` removed in JDK 24, `-Djava.security.manager=allow` rejected in JDK 24+, `-Djdk.reflect.useDirectMethodHandle=false` removed in JDK 22).

Goal: **Cassandra boots and runs on JDK 25** — all startup-fatal flags removed, the silent-security-regression call sites either fixed or fenced, build infrastructure teaches the tooling about JDK 25. Full test-green on JDK 25 (simulator, UDF sandbox semantics, in-JVM dtests that install a SecurityManager, etc.) is a larger follow-on and is called out below as explicit out-of-scope items with mitigations.

## Tooling caveat

Bash and subagent spawning were broken in the session that produced the original plan. A subsequent audit (with tooling restored) re-verified the hotspots below and flagged one additional silent-regression site (B8 — AuditLogManager) that the original plan missed. A full `grep -rn` audit across `src/` has now been performed; items not listed here were either verified no-op-acceptable or are warn-only on JDK 25.

---

## Audit findings

### A. Infrastructure (build + startup scripts + JVM options)

| # | File | Line | Issue | JDK 25 impact |
|---|---|---|---|---|
| A1 | `build.xml` | 48 | `java.supported="11,17,21"` | Ant fails at line 223–225 when invoked with JDK 25 |
| A2 | `build.xml` | 223–225 | `<fail>` if `ant.java.version` not in `java.supported` | Blocks compilation on JDK 25 |
| A3 | `build.xml` | 337–400 | `_jvm21_arg_items` with `-XX:+ZGenerational` (341) and `-Djava.security.manager=allow` (362) | Fatal on JDK 25 if re-used for 25 |
| A4 | `build.xml` | 398–400 | `java-jvmargs` set **only** for `ant.java.version` ∈ {11,17,21} with `else=""` | Empty jvm args on 25 → subsequent flag guards fail; `--add-exports`/`--add-opens` all missing |
| A5 | `build.xml` | 431–446 | `_jvm21_test_arg_items` contains `-Djava.security.manager=allow` (434) **and** `-Djdk.reflect.useDirectMethodHandle=false` (444, explicitly TODO'd for 25) | 434 fatal on JDK 25; 444 is ignored in JDK 22+ (silent test regression — FieldUtils cannot overwrite static final fields) |
| A6 | `build.xml` | 448–456 | `_std-test-jvmargs` conditional only for 11/17/21 | Tests on JDK 25 get no args (`-Dnet.bytebuddy.experimental=true`, `-Dio.netty.tryReflectionSetAccessible=true`, `--add-opens`/`--add-exports` all missing) |
| A7 | `build.xml` | 176 | `asm.version=9.5` | ASM 9.5 doesn't know JDK 25 class-file format v69; simulator instrumentation throws `IllegalArgumentException` when classloader sees a v69 class. (UDF path is safe — ECJ pins emitted class files to v55.) |
| A8 | `bin/cassandra.in.sh` | 117 | `java_versions_supported="11 17 21"` | Startup script refuses JDK 25 unless `CASSANDRA_JDK_UNSUPPORTED` set |
| A9 | `bin/cassandra.in.sh` | 143–150 | Cascading `if $JAVA_VERSION -ge 21 → jvm21-*.options` | JDK 25 silently inherits `jvm21-*.options` — hits the fatal flags in A10/A11 |
| A10 | `conf/jvm21-server.options` | 31–32 | `-XX:+UseZGC` + `-XX:+ZGenerational` | `-XX:+ZGenerational` removed in JDK 24 (JEP 490); fatal |
| A11 | `conf/jvm21-server.options` | 144 | `-Djava.security.manager=allow` | Rejected in JDK 24+ (JEP 486); fatal |
| A12 | `conf/jvm21-clients.options` | — | No breaking flags | Usable as-is |

### B. Source code — SecurityManager-dependent call sites

| # | File | Line | Issue |
|---|---|---|---|
| B1 | `src/java/org/apache/cassandra/security/ThreadAwareSecurityManager.java` | 48 | `public final class ThreadAwareSecurityManager extends SecurityManager` — compiles on JDK 25 (terminally deprecated) |
| B2 | same | 95 | `System.setSecurityManager(new ThreadAwareSecurityManager())` — **`UnsupportedOperationException` on JDK 24+**. If `ThreadAwareSecurityManager.install()` runs during daemon startup, the daemon dies. If wrapped, the UDF sandbox is silently absent and UDFs can execute arbitrary code. |
| B3 | same | 113 | `Policy.setPolicy(...)` — compiles; no-op on JDK 24+ |
| B4 | `src/java/org/apache/cassandra/auth/jmx/AuthorizationProxy.java` | 24–25 | imports `AccessController`, `AccessControlContext` |
| B5 | same | 165–166 | `AccessControlContext acc = AccessController.getContext(); Subject subject = Subject.getSubject(acc);` — **silent regression: `Subject.getSubject(acc)` returns null on JDK 24+.** `authorize()` at line 237 then treats the call as local-connector ("allow all") → **complete bypass of JMX authz**. |
| B6 | `src/java/org/apache/cassandra/utils/JMXServerUtils.java` | 49, 324–381 | Imports `javax.security.auth.Subject` and `JmxRegistry extends sun.rmi.registry.RegistryImpl`. Verified: `RegistryImpl` API stable across 11→25; covered by existing `--add-exports java.rmi/sun.rmi.registry=ALL-UNNAMED` in `jvm25-server.options:94`. No-op acceptable. |
| B7 | `src/java/org/apache/cassandra/cql3/functions/JavaBasedUDFunction.java` | 82 | Imports `ThreadAwareSecurityManager`; the entire `JavaBasedUDFunction` sandbox stack (ProtectionDomain/SecureClassLoader/PermissionCollection) relies on B2 being installed. The `UDFByteCodeVerifier` static bytecode deny-list is not exhaustive (e.g., does not block `System.exit`, file I/O, sockets). |
| B8 | `src/java/org/apache/cassandra/audit/AuditLogManager.java` | 496–497 | **Second `Subject.getSubject(AccessController.getContext())` call** — identical silent-null-on-JDK-24+ semantics to B5. This site sits in `JmxHandler.invoke` used by the `MBeanServerForwarder` when no custom `authorizer` is configured (`JMXServerUtils.java:142`). Consequence: every authenticated JMX call is audited with `user=null`. Does not bypass authz in this path but silently breaks audit-log integrity. Fix is identical one-liner to B5 and must ship in the same changeset. |

### C. Source code — other potentially affected areas (warn-only or verified-no-action)

- `conf/jvm-server.options:195` has `-Dnet.bytebuddy.experimental` with comment "Need experimental bytebuddy for JDK21" — still needed on 25, no change. The new `_jvm25_test_arg_items` must retain `-Dnet.bytebuddy.experimental=true`.
- Simulator `InterceptClasses` uses ASM `Opcodes` — when ASM is bumped to 9.8, **no source change** is needed: `Opcodes.ASM9` remains the latest API-version constant in ASM 9.8 (there is no `Opcodes.ASM10`). `FBUtilities.ASM_BYTECODE_VERSION = Opcodes.ASM9` and `InterceptClasses.BYTECODE_VERSION = Opcodes.ASM9` stay as-is.
- `AccessController.doPrivileged` sites are verified no-op acceptable on JDK 25 (the wrapper becomes a transparent identity; the lambda still runs):
  - `src/java/org/apache/cassandra/auth/jmx/AuthenticationProxy.java:107` — `subject.setReadOnly()` via `doPrivileged`.
  - `src/java/org/apache/cassandra/utils/FastByteOperations.java:192` — reflective `theUnsafe` grab.
- `sun.misc.Unsafe` memory-access methods are used in hot paths (`MemoryUtil`, `BigEndianMemoryUtil`, `LittleEndianMemoryUtil`, `Memory`, `FastByteOperations`, `Ref`). JEP 471/498 emits a runtime warning on first use on JDK 24+; **not yet fatal**. Out of scope for this plan; tracked separately.
- `sun.nio.ch.DirectBuffer` imports (7 files) — covered by `--add-opens java.base/sun.nio.ch=ALL-UNNAMED` in `jvm25-server.options:110`.
- `jdk.internal.ref.Cleaner` imports (`MemoryUtil`, `BufferPool`, `Ref`) — covered by `--add-opens java.base/jdk.internal.ref=ALL-UNNAMED` in `jvm25-server.options:108` (add-opens implies the equivalent add-exports).
- `src/java/org/apache/cassandra/utils/logging/LogbackLoggingSupport.java` — `ThreadAwareSecurityManager.isSecuredThread()` check is a pure-Java `ThreadGroup instanceof SecurityThreadGroup` test and works identically on JDK 25 regardless of SM install status.
- `src/java/org/apache/cassandra/utils/FBUtilities.java:1380` — reflective access to `jdk.internal.module.IllegalAccessLogger` inside a try/ignore. Harmless on JDK 25.
- `src/java/org/apache/cassandra/utils/binlog/BinLog.java:294` — production `public void finalize()` override. Deprecation-for-removal since JDK 18; still runs on JDK 25 with a warning. **Tracked follow-up**; not blocking for boot.
- `lib/ecj-3.33.0.jar` (Sep 2022) — Eclipse JDT compiler used for UDF compilation. UDFs are pinned to `CompilerOptions.VERSION_11` in `JavaBasedUDFunction.java:162,164`, so emitted class files are v55 (Java 11) and the ASM 9.5→9.8 bump is strictly for simulator instrumentation of JDK-25 runtime classes (v69), not for the UDF path. ECJ 3.33.0 itself must **run** on JDK 25; it has not been validated against 25 in this repo. Add as a verification step below.
- JNI surfaces (JNR `jnr-ffi-2.2.13`, `jna-5.13.0`, Netty tcnative, Snappy, `NativeLibraryLinux`/`NativeLibraryDarwin` via JNA) emit JEP 472 warnings on JDK 24+. Not fatal on 25. Optionally suppressed by adding `--enable-native-access=ALL-UNNAMED` to `jvm25-server.options`.

### C.2 Test-scope breakers (not blocking daemon boot; blocking CI on JDK 25)

These are surfaced so they are not overlooked during the JDK 25 CI bring-up, but they are explicitly **out of scope** for this plan's "runtime bootability" goal.

- `test/distributed/org/apache/cassandra/distributed/shared/PreventSystemExit.java:27` (`extends SecurityManager`) and its installers:
  `test/distributed/.../impl/Instance.java:1146,1161`, `AbstractCluster.java:1171`, `shared/ClusterUtils.java:1620`,
  `test/distributed/.../SSTableIdGenerationTest.java:92,106`,
  `test/unit/org/apache/cassandra/tools/ToolRunner.java:78,125`,
  `tools/sstableloader/test/unit/org/apache/cassandra/tools/LoaderOptionsTest.java:220,245`.
  Each `System.setSecurityManager(non-null)` throws `UnsupportedOperationException` on JDK 24+. Replacement strategy (ByteBuddy redefine of `Runtime.exit`, or classloader-scoped shutdown-hook capture) is a separate ticket.
- `System.runFinalization()` is a no-op since JDK 18 (JEP 421). Test sites silently false-pass on JDK 25:
  `test/distributed/.../upgrade/UpgradeTestBase.java:448`, `test/unit/.../binlog/BinLogTest.java:211`, `test/distributed/.../ResourceLeakTest.java:232,243,254,265,276,296`.
- `modules/accord/accord-core/.../AsyncChains.java:467` — `finalize()` override in a vendored module (warn-only, tracked separately with module owner).

---

## Recommended scope: "runtime bootability + silent-regression fix"

1. **Infra (all of A1–A11)** as described below.
2. **B5 + B8 fixed** — one-line swap to `Subject.current()` guarded by `Runtime.version().feature() >= 18`, applied at **both** `AuthorizationProxy.java:165–166` and `AuditLogManager.java:496–497`.
3. **B2 fenced** — `ThreadAwareSecurityManager.install()` detects JDK 24+, logs a loud WARN, returns without calling `setSecurityManager`, leaves `installed = false`.
4. **UDF gate** — `JavaBasedUDFunction` refuses to execute when `ThreadAwareSecurityManager.isInstalled() == false` unless `allow_insecure_udfs: true` is set in cassandra.yaml.

**Out of scope:**
- Rewriting UDF sandbox without SecurityManager (isolated classloader, GraalVM Isolate, external process) — needs a CEP.
- Replacing `sun.misc.Unsafe` memory methods with `VarHandle`/FFM.
- Suppressing JEP 472 JNI-use warnings in code (a JVM-flag workaround is offered below).
- Rewriting in-JVM dtest `PreventSystemExit` SecurityManager trap.
- Replacing `System.runFinalization()` test patterns with `Cleaner`/explicit release.
- `BinLog.java:294` / `AsyncChains.java:467` `finalize()` overrides.

---

## Implementation plan

### 1. Options files

**`conf/jvm25-server.options`** — **already exists in the repo.** Verify it is a copy of `conf/jvm21-server.options` minus `-XX:+ZGenerational` and `-Djava.security.manager=allow`. Optionally append `--enable-native-access=ALL-UNNAMED` to silence JEP 472 JNI warnings (cosmetic, forward-compatible).

**`conf/jvm25-clients.options`** — **already exists in the repo.** Verify it is a straight copy of `conf/jvm21-clients.options`.

### 2. `bin/cassandra.in.sh`

- Line 117: `java_versions_supported="11 17 21 25"`.
- Line 143–150: add a new highest branch for `-ge 25` before the existing `-ge 21`.

### 3. `build.xml`

- Line 48: `java.supported="11,17,21,25"`.
- Line 176: bump `asm.version` from `9.5` to `9.8`. No accompanying source change needed (`Opcodes.ASM9` constant stays).
- Add `_jvm25_arg_items` modeled on `_jvm21_arg_items` minus `-XX:+ZGenerational` and `-Djava.security.manager=allow`.
- Add `_jvm25_test_arg_items` modeled on `_jvm21_test_arg_items` minus `-Djava.security.manager=allow` and `-Djdk.reflect.useDirectMethodHandle=false`. **Retain `-Dnet.bytebuddy.experimental=true`.**
- Extend the `java-jvmargs` condition chain to cover 25.
- Extend the `_std-test-jvmargs` condition chain to cover 25.

### 4. Source-code fixes

**`src/java/org/apache/cassandra/auth/jmx/AuthorizationProxy.java`** (B5):

```java
Subject subject = Runtime.version().feature() >= 18
                  ? Subject.current()
                  : Subject.getSubject(AccessController.getContext());
```

Imports of `AccessController` / `AccessControlContext` stay (used on 17 path).

**`src/java/org/apache/cassandra/audit/AuditLogManager.java`** (B8):

Identical one-liner as B5, applied at line 496–497 inside `JmxHandler.invoke`:

```java
Subject subject = Runtime.version().feature() >= 18
                  ? Subject.current()
                  : Subject.getSubject(AccessController.getContext());
```

Imports of `AccessController` / `AccessControlContext` stay.

**`src/java/org/apache/cassandra/security/ThreadAwareSecurityManager.java`** (B2):

In `install()`, before `System.setSecurityManager(...)`:

```java
if (Runtime.version().feature() >= 24)
{
    logger.warn("Java SecurityManager is permanently disabled in JDK 24+ (JEP 486). " +
                "UDF sandbox is NOT active on this JVM. Set allow_insecure_udfs=true " +
                "in cassandra.yaml to acknowledge and proceed.");
    return;
}
```

Expose `public static boolean isInstalled() { return installed; }`.

**`src/java/org/apache/cassandra/cql3/functions/JavaBasedUDFunction.java`**: when executing a UDF, if `!ThreadAwareSecurityManager.isInstalled() && !DatabaseDescriptor.allowInsecureUDFs()`, throw with a clear error pointing at the flag.

### 5. NEWS.txt

Add a "JDK 25" subsection under 7.0 noting runtime support, the SecurityManager removal warning, and the `allow_insecure_udfs` requirement on JDK 24+ for UDFs.

---

## Verification

1. `JAVA_HOME=/path/to/jdk25 ant realclean build` — should succeed.
2. `JAVA_HOME=/path/to/jdk25 bin/cassandra -f` — starts; WARN about SM unavailable.
3. **JMX authz regression test** — an unauthenticated remote JMX call must be rejected, not silently allowed. Exercises B5.
4. **JMX audit regression test** — an authenticated remote JMX call must appear in the audit log with a non-null user field. Exercises B8.
5. **UDF gate** — without `allow_insecure_udfs=true`, creating/executing a Java UDF on JDK 25 should fail with the explicit error.
6. **ECJ-on-25 check** — a simple `CREATE FUNCTION ... LANGUAGE java` on JDK 25 must compile and execute (with `allow_insecure_udfs=true`). Confirms ECJ 3.33.0 boots on JDK 25.
7. **JDK 11 / 17 / 21 regression** — existing tests still green; the `>= 18` / `>= 24` guards keep older JDK paths unchanged.
8. `ant test-simulator` — confirms ASM 9.8 doesn't break bytecode instrumentation of JDK-25 runtime classes.
9. **Known failing tests on JDK 25** (not blocking this plan's merge; track separately):
   - Any in-JVM dtest or tool test that installs `PreventSystemExit`.
   - `ResourceLeakTest`, `BinLogTest`, `UpgradeTestBase` — `System.runFinalization()` is a no-op since JDK 18.

---

## Known remaining gaps

- Full SM-free UDF sandbox rewrite (needs CEP).
- `sun.misc.Unsafe` memory-method migration to `VarHandle` / FFM.
- JEP 472 JNI warnings in code (suppressed by `--enable-native-access=ALL-UNNAMED` at the JVM level for now).
- In-JVM dtest `PreventSystemExit` SecurityManager rewrite (ByteBuddy redefine of `Runtime.exit`, or classloader-scoped shutdown-hook capture).
- `System.runFinalization()` tests: `ResourceLeakTest` / `BinLogTest` / `UpgradeTestBase` — re-design to use `Cleaner` or explicit release.
- `BinLog.java:294` and `modules/accord/accord-core/.../AsyncChains.java:467` — `finalize()` overrides (warn-only on JDK 25).
- ECJ 3.33.0 → 3.40+ bump (conservative; not strictly required since emitted UDF bytecode is pinned to v55).
