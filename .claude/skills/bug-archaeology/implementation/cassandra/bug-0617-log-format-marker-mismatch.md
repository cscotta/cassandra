# Bug 0617: Log Format String Has One Extra Placeholder Without Matching Argument

**Commit**: 88c0e29caa · **Ticket**: n/a
**Subsystem**: `schema/MigrationManager.java`
**Category**: Wrong constant / default value

---

## Root Cause

The SLF4J `logger.debug()` call in `MigrationManager` contained two `{}` placeholders in its format string but only provided one corresponding argument (`endpoint`). SLF4J silently drops the extra placeholder text and may emit a warning, but the intent was to remove a now-absent parameter reference that had been left dangling after an earlier refactoring. The mismatch between placeholder count and argument count produces incorrect or incomplete log output.

### Before / After

```java
// BEFORE
logger.debug("Immediately submitting migration task for {} due to {}, " +
             "schema versions: local={}, remote={}",
             endpoint,
             Schema.schemaVersionToString(Schema.instance.getVersion()),
             ...);
```

```java
// AFTER
logger.debug("Immediately submitting migration task for {}, " +
             "schema versions: local={}, remote={}",
             endpoint,
             Schema.schemaVersionToString(Schema.instance.getVersion()),
             ...);
```

---

## How to Spot

Count the number of `{}` tokens in every SLF4J format string and compare against the number of additional arguments supplied to the logging call. A mismatch is almost always a cut-and-paste or refactoring error. Static analysis (PMD, SpotBugs, or the `slf4j` Maven plugin) can detect this automatically.

---

## Test

No test added.
