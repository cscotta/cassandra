# Bug 0990: Log Format Strings Have More Placeholders Than Arguments

**Commit**: 2188bec560 · **Ticket**: n/a
**Subsystem**: Hints
**Category**: Wrong constant / default value

---

## Root Cause
Three `logger.warn`/`logger.error` calls in `HintVerbHandler` and `LegacyHintsMigrator` contained more `{}` placeholders in the format string than there were arguments supplied. SLF4J silently leaves extra placeholders unreplaced at runtime, meaning log messages showed literal `{}` instead of the intended values, and in some cases the exception was treated as a parameter rather than being logged as a cause. The fix aligns the format strings with the actual arguments.

### Before / After
```java
// BEFORE — "table id {}" has no matching argument
logger.warn("Failed to validate a hint for {} (table id {}) - skipped", hostId);

// AFTER
logger.warn("Failed to validate a hint for {} - skipped", hostId);

// BEFORE — trailing ": {}" consumed the exception as a parameter
logger.error("Failed to migrate a hint for {} from legacy {}.{} table: {}",
             targetId, SystemKeyspace.NAME, SystemKeyspace.LEGACY_HINTS, e);

// AFTER — exception passed as cause, not as format arg
logger.error("Failed to migrate a hint for {} from legacy {}.{} table",
             targetId, SystemKeyspace.NAME, SystemKeyspace.LEGACY_HINTS, e);
```

---

## How to Spot
Count `{}` occurrences in every SLF4J format string and compare against the number of additional arguments. Static analysis tools (e.g., SpotBugs SLF4J plugin) catch this automatically. Pay special attention to catch blocks where the exception is the last argument — if the format string ends with `": {}"` the exception is treated as a replacement value rather than as a Throwable cause.

---

## Test
No test added.
