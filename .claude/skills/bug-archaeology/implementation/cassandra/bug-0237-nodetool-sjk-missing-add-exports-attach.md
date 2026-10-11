# Bug 0237: nodetool/sjk Heap Histogram Fails on Java 17 Due to Missing --add-exports

**Commit**: de1ff6d4d1 · **Ticket**: CASSANDRA-18242
**Subsystem**: conf/jvm17-clients.options
**Category**: Wrong constant / default value

---

## Root Cause

The `nodetool` and `sjk` heap-histogram (`hh`) commands use `sun.tools.attach`, which lives in the `jdk.attach` module. On JDK17 this internal package is not exported by default, so any reflective or direct access to `sun.tools.attach` throws `InaccessibleObjectException`. The `--add-exports jdk.attach/sun.tools.attach=ALL-UNNAMED` flag was absent from `conf/jvm17-clients.options`, the options file loaded for client-side tools.

### Before / After

```
# BEFORE (conf/jvm17-clients.options — missing entry)
--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
```

```
# AFTER (conf/jvm17-clients.options)
--add-exports jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-exports jdk.attach/sun.tools.attach=ALL-UNNAMED
```

---

## How to Spot

Client tool JVM options (`jvm17-clients.options`) must mirror every `--add-exports` or `--add-opens` needed by tools that attach to or introspect a running JVM. When enabling a new diagnostic subcommand that depends on `jdk.attach`, review both the server and client options files to ensure the export is present in both.

---

## Test

No test added; the fix is verified by running `nodetool hh` against a JDK17 node and confirming no `InaccessibleObjectException` is thrown.
