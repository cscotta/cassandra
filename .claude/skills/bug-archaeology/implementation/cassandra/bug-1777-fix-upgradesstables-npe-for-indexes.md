# Bug 1777: Fix upgradesstables NPE for non-CF-based indexes

**Commit**: 45955e6829 · **Ticket**: CASSANDRA-6645
**Subsystem**: db/index (SecondaryIndexSearcher, StorageService)
**Category**: Missing null / bounds check

---

## Root Cause

Same root cause as Bug 1776 applied to a different branch of the codebase: `SecondaryIndex.getIndexCfs()` can return `null` for non-CF-based index implementations. The base class `SecondaryIndexSearcher.highestSelectivityPredicate()` iterated candidate indexes and called `index.getIndexCfs().getMeanColumns()` without a null check, and `StorageService` similarly added null CFS entries to the upgrade list. This is the earlier branch-specific commit addressing CASSANDRA-6645.

### Before / After

```java
// BEFORE (SecondaryIndexSearcher)
if (index == null || (expression.op != IndexOperator.EQ))
    continue;
int columns = index.getIndexCfs().getMeanColumns();
```

```java
// AFTER (SecondaryIndexSearcher)
if (index == null || index.getIndexCfs() == null || expression.op != IndexOperator.EQ)
    continue;
int columns = index.getIndexCfs().getMeanColumns();
```

---

## How to Spot

When a collection is filtered with a null check for the object itself but not for the nullable return value of a method on that object, the null can slip through to the next call site. Review filter conditions to ensure all nullable accessors are also guarded.

---

## Test

No test added.
