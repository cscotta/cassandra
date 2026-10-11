# Bug 1417: Fix SSTable Read Rate Tracking for NamesQueryFilters

**Commit**: 684b4f96fa · **Ticket**: CASSANDRA-8239
**Subsystem**: db/CollationController
**Category**: Silent skip on non-match

---

## Root Cause

`CollationController` has two read paths: the general slice-based path and the narrower `NamesQueryFilter` path (point lookups by exact column name). The general path called `sstable.incrementReadCount()` before iterating, but the `NamesQueryFilter` branch skipped that call entirely. As a result, read-rate metrics (used for cache warmth tracking and hot-row detection) were never updated for name-query reads, making the read meter systematically undercount for that query type.

### Before / After

```java
// BEFORE — incrementReadCount() absent in NamesQueryFilter branch
Tracing.trace("Merging data from sstable {}", sstable.descriptor.generation);
OnDiskAtomIterator iter = reducedFilter.getSSTableColumnIterator(sstable);
```

```java
// AFTER — read count incremented before iterating
Tracing.trace("Merging data from sstable {}", sstable.descriptor.generation);
sstable.incrementReadCount();
OnDiskAtomIterator iter = reducedFilter.getSSTableColumnIterator(sstable);
```

---

## How to Spot

When a method has multiple execution paths that share a common postcondition (e.g., "every sstable read must be metered"), check that each branch fulfills it independently. A quick grep for the metering call across all branches of the same method reveals asymmetric coverage. Code reviews should treat instrumentation calls (metrics, tracing, rate tracking) as correctness requirements, not optional decoration, and verify they appear uniformly across all exit paths.

---

## Test

`SSTableReaderTest.testReadRateTracking` writes 10 rows, flushes, then performs a slice read, a named-column slice read, and a `NamesQueryFilter` read in sequence, asserting the read meter count increments to 1, 2, and 3 respectively.
