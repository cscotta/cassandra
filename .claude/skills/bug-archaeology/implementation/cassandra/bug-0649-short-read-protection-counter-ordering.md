# Bug 0649: Short Read Protection Fetches Rows Too Aggressively

**Commit**: f93e6e3401 · **Ticket**: CASSANDRA-13794
**Subsystem**: src/java/org/apache/cassandra/service/DataResolver.java
**Category**: Logic error in condition

---

## Root Cause

The original short-read-protection (SRP) logic used a single shared `DataLimits.Counter` for both tracking per-replica row counts and the final merged result, and applied the counter transformation before the `MoreRows` extension. This caused the counting to be applied out of order: the `applyToRow()` callback was skipped for the first row of each extension batch, leading to inaccurate counts. The retry formula (`n²/x - n`) also fired even when the replica had returned far fewer rows than needed, generating unnecessary round trips. The refactored fix separates the per-source counter from the merged-result counter, corrects the transformation ordering (extend first, then apply protection and counter), and uses a simpler, bounded query size.

### Before / After

```java
// BEFORE – single counter shared for per-source and merged views
partition = Transformation.apply(partition, counter);       // count first
partition = MoreRows.extend(partition, protection);
partition = Transformation.apply(partition, protection);
```

```java
// AFTER – separate counters; extend before applying transformations
return Transformation.apply(MoreRows.extend(partition, protection), protection);
// singleResultCounter applied outside in mergeWithShortReadProtection()
```

---

## How to Spot

Whenever a stream-processing pipeline chains `MoreRows.extend()` with `Transformation.apply()`, verify that the ordering matches the desired callback sequence: transformations applied before `extend()` will not receive events from rows produced by the extension. Additionally, be suspicious of any metric-based retry formula that can produce values larger than the page size — a simple bounded constant is often safer.

---

## Test

No test added (performance and correctness validated via existing integration tests).
