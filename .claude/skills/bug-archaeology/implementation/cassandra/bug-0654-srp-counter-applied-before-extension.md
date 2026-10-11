# Bug 0654: Short Read Protection Counter Applied Before MoreRows Extension

**Commit**: 9ea61305ec · **Ticket**: CASSANDRA-12872
**Subsystem**: src/java/org/apache/cassandra/service/DataResolver.java
**Category**: Logic error in condition

---

## Root Cause

`ShortReadProtection.applyToPartition()` applied the per-source `counter` transformation to the partition iterator first, then extended it with `MoreRows` and finally applied the `protection` tracking transformation. Because the counting transformation was added before the `MoreRows` extension, its `applyToRow()` was never called on the first row returned by each subsequent extension iterator — those rows entered the pipeline after the counting transformation's position in the chain. This caused the counter to under-count rows, making `moreContents()` believe more rows were needed than actually were, and potentially issuing unnecessary (or incorrect) retry reads.

### Before / After

```java
// BEFORE – counter applied before MoreRows; misses first row of each extension
partition = Transformation.apply(partition, counter);
partition = MoreRows.extend(partition, protection);
partition = Transformation.apply(partition, protection);
```

```java
// AFTER – extend first so all transformations see every row including extension rows
partition = MoreRows.extend(partition, protection);
partition = Transformation.apply(partition, protection); // track lastClustering
partition = Transformation.apply(partition, counter);    // count after tracking
```

---

## How to Spot

In Cassandra's `Transformation`/`MoreRows` pipeline, any transformation added to an iterator before `MoreRows.extend()` will not receive callbacks for rows produced by the extension. Review every `applyToPartition()` that combines both `MoreRows.extend()` and `Transformation.apply()` to ensure that stateful transformations are chained after the extension, not before.

---

## Test

No test added.
