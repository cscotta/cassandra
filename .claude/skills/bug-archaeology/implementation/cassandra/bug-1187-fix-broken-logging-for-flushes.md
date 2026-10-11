# Bug 1187: Wrong Format Specifier in Memtable Empty-Flush Log Message

**Commit**: 1eda7cb55c · **Ticket**: CASSANDRA-9837
**Subsystem**: src/java/org/apache/cassandra/db/Memtable.java
**Category**: Wrong constant / default value

---

## Root Cause

The log statement for a completed "empty" flush (where no SSTable needed to be written) used `%s` — a `String.format`-style placeholder — as the first argument to an SLF4J `logger.info` call. SLF4J uses `{}` as its substitution token; `%s` is treated as a literal string, so the filename was never interpolated. The log line would always print the literal text `%s` instead of the actual SSTable path, making it impossible to trace which memtable completed the empty flush.

### Before / After

```java
// BEFORE
logger.info("Completed flushing %s; nothing needed to be retained.  Commitlog position was {}",
            writer.getFilename(), context);
```

```java
// AFTER
logger.info("Completed flushing {}; nothing needed to be retained.  Commitlog position was {}",
            writer.getFilename(), context);
```

---

## How to Spot

Mixed use of `String.format` placeholders (`%s`, `%d`) and SLF4J placeholders (`{}`) in the same codebase is a persistent hazard. When reviewing log statements, verify that every `logger.*` call uses `{}` for substitution, not `%s`. A simple grep for `logger\.\(debug\|info\|warn\|error\).*%[sdf]` will surface every occurrence.

---

## Test

No test added.
