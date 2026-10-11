# Bug 2016: Spurious `without()` Method Introduced by Bad Merge in RowMutation

**Commit**: f5cc4760b9 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/RowMutation.java
**Category**: Logic error in condition

---

## Root Cause

A bad branch merge introduced a `without(UUID cfId)` method into `RowMutation` that did not belong in the merged result. The method constructed a copy of the mutation excluding the named column family, but was not part of the intended codebase at that point and its presence created an incorrect API surface that callers could inadvertently rely on. The fix simply removes the nine lines that were incorrectly introduced.

### Before / After

```java
// BEFORE
public RowMutation without(UUID cfId)
{
    RowMutation rm = new RowMutation(table, key);
    for (Map.Entry<UUID, ColumnFamily> entry : modifications.entrySet())
        if (!entry.getKey().equals(cfId))
            rm.add(entry.getValue());
    return rm;
}
```

```java
// AFTER
// method removed entirely
```

---

## How to Spot

After resolving a merge conflict, review every added method or field in the merged file against the target branch's intended API. Methods that appear only in one branch but have no callers in the merged result are prime candidates for erroneous inclusion.

---

## Test

No test added.
