# Bug 1280: Empty List Delete Throws False "List Is Null" Error

**Commit**: f56244d21a · **Ticket**: CASSANDRA-9198
**Subsystem**: src/java/org/apache/cassandra/cql3/Lists.java
**Category**: Missing null / bounds check

---

## Root Cause

`UpdateParameters.getPrefetchedList()` could return `null` when a CQL list column existed in the schema but had no stored cells (e.g., all elements had been removed). The callers in `Lists.Setter` and `Lists.Discarder` checked only for `null` before attempting index bounds validation, so an empty-but-non-null list from a previous delete would produce a misleading exception. The deeper issue was that `getPrefetchedList` itself did not normalize a `null` cell list from `CQL3Row.getMultiCellColumn()` to an empty list, leaving a null-vs-empty ambiguity that callers could not reliably distinguish.

### Before / After

```java
// BEFORE — getPrefetchedList returns null for empty lists;
// callers treat null == "list column doesn't exist" and throw
if (existingList == null)
    throw new InvalidRequestException("Attempted to delete an element from a list which is null");
```

```java
// AFTER — normalize null to empty list in getPrefetchedList;
// callers treat both null and size==0 as "no element to delete" gracefully
List<Cell> cql3List = row.getMultiCellColumn(cql3ColumnName);
return (cql3List == null) ? Collections.<Cell>emptyList() : cql3List;

// ...and in the caller:
if (existingList == null || existingList.size() == 0)
    throw new InvalidRequestException("Attempted to delete an element from a list which is null");
```

---

## How to Spot

Whenever code checks `collection == null` as a proxy for "does not exist" but a separate code path can produce an empty (non-null) collection for the same semantic state, the null check becomes incomplete. Review every place a prefetched or lazily-loaded collection is null-checked before index access: ask whether the backing store distinguishes absent from empty, and whether the accessor normalizes that distinction before returning.

---

## Test

`CollectionsTest` extended to verify that deleting from an already-emptied list and removing a non-existent map key both succeed without throwing, and that the row reflects `null` afterwards.
