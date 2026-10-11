# Bug 0477: Duplicate Rows Created During 2.x-to-3.x Major Upgrade

**Commit**: 4d42c189fa · **Ticket**: CASSANDRA-15789
**Subsystem**: `src/java/org/apache/cassandra/db/LegacyLayout.java`, `src/java/org/apache/cassandra/db/partitions/PartitionUpdate.java`, `src/java/org/apache/cassandra/db/transform/DuplicateRowChecker.java`
**Category**: Logic error in condition

---

## Root Cause

`LegacyLayout.CellGrouper#addRowTombstone` had a guard that only updated the row deletion when `rowDeletion != null`, meaning a row-level deletion tombstone was silently dropped the first time it was seen for a row that had not yet received any deletion. Additionally, when `PartitionUpdate` was built from a legacy (pre-3.0) deserialized partition, it used the standard `fromIterator` path that assumed an ordered, duplicate-free input and silently discarded later rows with the same clustering. The fix corrects the null-guard in `addRowTombstone` and introduces `fromPre30Iterator`, which passes a `QuickResolver` merging duplicate clusterings via `Rows.merge` rather than dropping them. A new `DuplicateRowChecker` transformation was also added to compaction and read paths to detect and optionally snapshot tables exhibiting this condition.

### Before / After

```java
// BEFORE — in addRowTombstone
if (rowDeletion != null && clustering.equals(tombstone.start.getAsClustering(metadata))) {
    if (tombstone.deletionTime.supersedes(rowDeletion.deletionTime)) { ... }
```

```java
// AFTER
if (clustering.equals(tombstone.start.getAsClustering(metadata))) {
    if (rowDeletion == null || tombstone.deletionTime.supersedes(rowDeletion.deletionTime)) { ... }
```

---

## How to Spot

When accumulating state that starts as null and is set on first encounter, guards of the form `if (field != null && ...)` incorrectly skip the first-encounter case. Review any accumulators on nullable fields where the initialisation and the update share the same conditional branch. Additionally, any deserialization path that accepts 2.x data should use a resolver-aware builder since 2.x format does not guarantee unique clusterings per partition.

---

## Test

`MixedModeReadRepairTest` runs a 2.2-to-3.0 upgrade scenario with read-repair enabled, asserting that reads after the upgrade do not return duplicate rows; `DuplicateRowCheckerTest` and `CompactionIteratorTest` unit-test the new checker and the merge resolver.
