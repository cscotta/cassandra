# Bug 0149: ClassCastException When Verifying Tables with MetaStrategy Partitioner

**Commit**: 27c1e56e43 · **Ticket**: CASSANDRA-19710
**Subsystem**: io/sstable/format (SortedTableVerifier), db/compaction (CompactionManager)
**Category**: Logic error in condition

---

## Root Cause

`SortedTableVerifier.verify()` skips the token-ownership check for tables using `LocalPartitioner` (because it has a non-standard, non-ring-based token space) but did not also skip it for tables using `MetaStrategy.partitioner` (the cluster metadata keyspace). `MetaStrategy.partitioner` is a reversed `LocalPartitioner`; when the verifier tried to cast its tokens to the regular `Token` type expected by the ring, it threw a `ClassCastException`. Similarly, `CompactionManager.performCleanup()` was checking `replication.isMeta()` to identify MetaStrategy keyspaces, but a `ClassCastException` could still occur because the partitioner comparison is more direct and reliable. Both sites were updated to use `cfs.getPartitioner() == MetaStrategy.partitioner` instead.

### Before / After

```java
// BEFORE – only LocalPartitioner excluded; MetaStrategy partitioner causes ClassCastException
if (options.checkOwnsTokens && !isOffline && !(cfs.getPartitioner() instanceof LocalPartitioner))
```

```java
// AFTER – MetaStrategy partitioner also excluded
if (options.checkOwnsTokens && !isOffline && !(cfs.getPartitioner() instanceof LocalPartitioner) && !(cfs.getPartitioner() == MetaStrategy.partitioner))
```

---

## How to Spot

Any code that enumerates token ownership or performs ring-based operations on column family stores must guard against non-standard partitioners, not just `LocalPartitioner`. When new partitioner types are introduced (such as `MetaStrategy.partitioner`), all existing partitioner-dispatch guards should be audited to ensure the new type is covered.

---

## Test

Unit test inserts rows into `system.local_metadata_log` (which uses the MetaStrategy partitioner), flushes to SSTable, then runs `IVerifier` with `checkOwnsTokens=true` and asserts no exception is thrown.
