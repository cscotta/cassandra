# Bug 1372: Wrong Dividend Used When Calculating Expected SSTable Write Size During Compaction

**Commit**: 4e1e92b310 · **Ticket**: CASSANDRA-8532
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionTask.java
**Category**: Wrong constant / default value

---

## Root Cause

`CompactionTask` chose the write directory by calling `getWriteDirectory()` with an estimated per-SSTable byte count. The argument passed was `estimatedTotalKeys / estimatedSSTables`—a key count, not a byte size—while `getWriteDirectory()` expects bytes in order to select a disk with sufficient free space. Passing a key count instead of a byte estimate could route compaction output to a disk that lacked room for the actual data, or skip disks that were large enough. The fix computes the expected SSTable size in bytes via `getExpectedWriteSize()` capped at `maxSSTableBytes`, and passes that to `getWriteDirectory()`.

### Before / After

```java
// BEFORE: wrong unit passed—this is a key count, not a byte estimate
writer.switchWriter(createCompactionWriter(
    cfs.directories.getLocationForDisk(getWriteDirectory(estimatedTotalKeys / estimatedSSTables)),
    keysPerSSTable, minRepairedAt));
```

```java
// AFTER: correct byte estimate used
long expectedSSTableSize = Math.min(getExpectedWriteSize(), strategy.getMaxSSTableBytes());
writer.switchWriter(createCompactionWriter(
    cfs.directories.getLocationForDisk(getWriteDirectory(expectedSSTableSize)),
    keysPerSSTable, minRepairedAt));
```

---

## How to Spot

When a numeric value is threaded through several layers of abstraction, watch for unit mismatches at the call site. If a method is named `getWriteDirectory(long sizeInBytes)` but the argument expression is a quotient of two key-count values, the types are semantically wrong even if both are `long`. Code review should flag any division of a `keys` estimate being fed directly to a `bytes`-expecting parameter.

---

## Test

No test added.
