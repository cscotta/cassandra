# Bug 1104: Fix TransactionLog Recovery lastModified Bug

**Commit**: b5b32a4291 · **Ticket**: CASSANDRA-10159
**Subsystem**: src/java/org/apache/cassandra/db/lifecycle/TransactionLog.java
**Category**: Logic error in condition

---

## Root Cause

During TransactionLog recovery, the verification step compared `updateTime` against the on-disk record unconditionally, even when there were no files present for the OLD record (i.e., `currentRecord.numFiles == 0`). When all files for an OLD entry have already been cleaned up, `Record.makeOld` produces an `updateTime` of 0 (the epoch default), which will never match the recorded timestamp and falsely triggers an error. The check should only apply when files actually exist on disk to compare against.

### Before / After

```java
// BEFORE
if (updateTime != currentRecord.updateTime)
{
    logger.error("Possible disk corruption detected ...");
```

```java
// AFTER
if (updateTime != currentRecord.updateTime && currentRecord.numFiles > 0)
{
    logger.error("Unexpected files detected ...");
```

---

## How to Spot

When a verification check computes a derived value from a potentially empty set (files, records, measurements) and then compares that derived value against a stored reference, always check whether the set being queried is empty before treating a mismatch as an error. A default/zero value produced from nothing is not a meaningful baseline for comparison. The guard `&& currentRecord.numFiles > 0` is the canonical pattern for "only validate when there is something to validate against."

---

## Test

The test suite adds two variants of `testGetTemporaryFilesSafeAfterObsoletion` (finish-before and finish-after) and hardens the `sstable` helper to avoid false-sharing between tests. The existing transaction log tests cover the recovery path indirectly.
