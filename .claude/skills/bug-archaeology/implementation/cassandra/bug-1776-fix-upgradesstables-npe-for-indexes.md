# Bug 1776: Fix upgradesstables NPE for non-CF-based indexes

**Commit**: 8b6d87b86b · **Ticket**: CASSANDRA-6645
**Subsystem**: db/index (SecondaryIndex, StorageService)
**Category**: Missing null / bounds check

---

## Root Cause

Some `SecondaryIndex` implementations (non-CF-based indexes) do not have an associated `ColumnFamilyStore` and return `null` from `getIndexCfs()`. The `upgradesstables` code path in `StorageService` iterated over all indexes and unconditionally called `valid.add(si.getIndexCfs())`, adding a `null` entry. The searcher classes (`CompositesSearcher`, `KeysSearcher`) then called methods on that null, crashing with an NPE.

### Before / After

```java
// BEFORE (StorageService)
for (SecondaryIndex si : cfStore.indexManager.getIndexes())
{
    logger.info("adding secondary index {} to operation", si.getIndexName());
    valid.add(si.getIndexCfs());
}
```

```java
// AFTER (StorageService)
for (SecondaryIndex si : cfStore.indexManager.getIndexes())
{
    if (si.getIndexCfs() != null)
    {
        logger.info("adding secondary index {} to operation", si.getIndexName());
        valid.add(si.getIndexCfs());
    }
}
```

---

## How to Spot

Whenever iterating a heterogeneous collection of interface implementations (here `SecondaryIndex`) and calling a method that may legitimately return `null` for some subtypes, always guard the result before use. The guard should be placed at every call site, not just one.

---

## Test

No test added.
