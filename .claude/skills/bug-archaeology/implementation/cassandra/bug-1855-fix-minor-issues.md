# Bug 1855: Fix Minor Logging Issues

**Commit**: 863428c2b5 · **Ticket**: n/a
**Subsystem**: db/compaction/Scrubber, service/StorageService
**Category**: Logic error in condition

---

## Root Cause

Two log call sites mixed SLF4J parameterized formatting with Java string concatenation incorrectly. In `Scrubber.java`, the format string used `{}` as a literal suffix via `+` concatenation (`"Reading row at {}" + rowStart`), which means SLF4J received a fully-assembled string with no substitution marker, so the row offset was never included in the output. In `StorageService.java`, the inverse mistake was present: a multi-line string concatenation was used instead of an SLF4J `{}` placeholder, causing eager string construction even when the log level was disabled.

### Before / After

```java
// BEFORE (Scrubber.java)
outputHandler.debug("Reading row at {}" + rowStart);

// BEFORE (StorageService.java)
logger.info("Received unexpected REPLICATION_FINISHED message from " + node
             + ". Was this node recently a removal coordinator?");
```

```java
// AFTER (Scrubber.java)
outputHandler.debug("Reading row at " + rowStart);

// AFTER (StorageService.java)
logger.info("Received unexpected REPLICATION_FINISHED message from {}. Was this node recently a removal coordinator?", node);
```

---

## How to Spot

SLF4J parameterized logging and `+` string concatenation must not be mixed in the same log call. Lint rules or static analysis (e.g., SpotBugs SLF4J plugin) catch this automatically. When reviewing log statements, check that the number of `{}` markers matches the number of trailing arguments.

---

## Test

No test added.
