# Bug 1553: Fix setlogginglevel Help Text in NodeTool

**Commit**: 43c2c169b7 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/tools/NodeTool.java
**Category**: Wrong constant / default value

---

## Root Cause

The `@Command` and `@Arguments` annotations on the `SetLoggingLevel` command used stale parameter names (`classQualifer`, `level`) that did not match the actual argument semantics or the updated help YAML. The annotation text is surfaced directly in the CLI `--help` output, so users saw incorrect parameter names and an inconsistent description. The fix updates both the command description and the `@Arguments` usage string to use the correct, user-facing names (`<class>` and `<level>`).

### Before / After

```java
// BEFORE
@Command(name = "setlogginglevel", description = "Set a log level for a given logger. If both classQualifer and level are empty/null, it will reset based on the initial configuration")
...
@Arguments(usage = "<classQualifer> <level>", description = "The logger classQualifer and the logger level (can be empty)")
```

```java
// AFTER
@Command(name = "setlogginglevel", description = "Set the log level threshold for a given class. If both class and level are empty/null, it will reset to the initial configuration")
...
@Arguments(usage = "<class> <level>", description = "The class to change the level for and the log level threshold to set (can be empty)")
```

---

## How to Spot

When renaming CLI parameters or changing their semantics, check all annotation strings (`@Command`, `@Arguments`, `@Option`) in addition to runtime logic — they are not validated by the compiler and can silently lag behind the implementation.

---

## Test

No test added.
