# Bug 1601: Fix CqlPagingRecordReader on Tables with Few Rows

**Commit**: 30df4a7283 · **Ticket**: CASSANDRA-7322
**Subsystem**: src/java/org/apache/cassandra/service
**Category**: Wrong constant / default value

---

## Root Cause

`StorageService.getSplits()` used `cfs.estimatedKeysForRange(range)` directly as the key-count estimate for each split. For tables with very few rows the SSTable-based estimator can return 0, causing `CqlPagingRecordReader` to skip the split entirely (treating it as empty). The fix clamps the estimate from below to `cfs.metadata.getMinIndexInterval()`, ensuring every split always reports at least one key and is therefore processed by the reader.

### Before / After

```java
// BEFORE
splits.add(Pair.create(range, cfs.estimatedKeysForRange(range)));
```

```java
// AFTER
// always return an estimate > 0 (see CASSANDRA-7322)
splits.add(Pair.create(range, Math.max(cfs.metadata.getMinIndexInterval(), cfs.estimatedKeysForRange(range))));
```

---

## How to Spot

When a consumer filters or skips work based on an estimated count, verify what happens when the estimate is zero. If the estimator is heuristic (e.g., SSTable sampling), a lower bound of at least 1 (or a configurable minimum) prevents silent data loss.

---

## Test

No test added.
