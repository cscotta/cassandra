# Bug 0234: JDK17 Startup Failure Due to Missing --add-opens for java.net

**Commit**: d1d733bee5 · **Ticket**: CASSANDRA-18431
**Subsystem**: conf/jvm17-server.options, build.xml
**Category**: Wrong constant / default value

---

## Root Cause

After CASSANDRA-17199 updated jamm (Java Agent for Memory Measurements) to a version that walks `java.net` types for heap-size accounting, the `--add-opens java.base/java.net=ALL-UNNAMED` flag was missing from both the server JVM options file and the in-JVM test harness arguments in `build.xml`. On JDK17 the JVM's strong encapsulation blocks reflective access to non-exported packages by default, so jamm throws an `InaccessibleObjectException` at startup.

### Before / After

```
# BEFORE (conf/jvm17-server.options — missing entry)
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
```

```
# AFTER (conf/jvm17-server.options)
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
#jamm post CASSANDRA-17199
--add-opens java.base/java.net=ALL-UNNAMED
```

---

## How to Spot

Whenever a dependency is upgraded and it adds reflective access to a new JDK internal package, the corresponding `--add-opens` (or `--add-exports`) line must be added to every JVM options file that runs on JDK17+. Review the diff of the dependency upgrade for new `getDeclaredFields`/`getDeclaredMethods` call-sites and cross-check against the existing module-open list in the relevant `.options` file.

---

## Test

No test added; the fix is verified by confirming a successful node startup under JDK17 after the dependency bump.
