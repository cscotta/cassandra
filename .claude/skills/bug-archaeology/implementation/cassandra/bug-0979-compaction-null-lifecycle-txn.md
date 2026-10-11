# Bug 0979: NPE in SSTable Operations When No SSTables Are Compacting

**Commit**: 101cd72e55 · **Ticket**: CASSANDRA-10980
**Subsystem**: Compaction
**Category**: Missing null / bounds check

---

## Root Cause
`CompactionManager.parallelAllSSTableOperation()` obtains a `LifecycleTransaction` by calling `cfs.markAllCompacting()`, which can legitimately return `null` when all SSTables are already participating in another operation. The original code called `operation.filterSSTables(compacting)` unconditionally, which causes a NullPointerException when `compacting` is `null`. This affects bulk SSTable operations such as scrub, verify, and upgrade-sstables.

The fix adds a null check: when `markAllCompacting()` returns `null`, the iterable of SSTables is substituted with an empty list, so the loop body is skipped and the operation exits cleanly with a log message rather than crashing.

### Before / After
```java
// BEFORE – NPE if markAllCompacting returns null
try (LifecycleTransaction compacting = cfs.markAllCompacting(operationType);)
{
    Iterable<SSTableReader> sstables = Lists.newArrayList(operation.filterSSTables(compacting));

// AFTER – null-safe empty list substitution
try (LifecycleTransaction compacting = cfs.markAllCompacting(operationType))
{
    Iterable<SSTableReader> sstables = compacting != null
        ? Lists.newArrayList(operation.filterSSTables(compacting))
        : Collections.<SSTableReader>emptyList();
```

---

## How to Spot
Whenever a method that acquires or marks a resource can return `null` (rather than throwing), every call site must check the return value before use. The try-with-resources construct does not protect against null — it only ensures `close()` is called on a non-null object. Review all callers of `markAllCompacting()` and similar acquisition methods for missing null guards.

---

## Test
No test added.
