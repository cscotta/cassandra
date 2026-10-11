# Bug 0980: Stale Secondary Index Entries Not Purged on Read

**Commit**: 06960d752a · **Ticket**: CASSANDRA-11013
**Subsystem**: Secondary indexes
**Category**: Missing override of default method

---

## Root Cause
`CompositesSearcher` detects stale secondary index entries while scanning results and accumulates them in a `staleEntries` list for lazy deletion. The deletion was supposed to happen when each base-table partition is closed, by overriding the partition iterator's close callback. However, the method was overriding `onClose()` instead of the correct interface method `onPartitionClose()`. Because the wrong method was overridden, `deleteAllEntries()` was never called and stale index entries were never purged during reads, allowing tombstoned or updated rows to keep appearing in index-based queries.

### Before / After
```java
// BEFORE – wrong method name, override never fires
@Override
public void onClose()
{
    deleteAllEntries(staleEntries, writeOp, nowInSec);
}

// AFTER – correct interface method
@Override
public void onPartitionClose()
{
    deleteAllEntries(staleEntries, writeOp, nowInSec);
}
```

---

## How to Spot
When implementing an interface callback for cleanup or finalization logic, verify the exact method name against the interface declaration — especially when interfaces have similarly-named methods (`onClose`, `onPartitionClose`, `close`). The compiler will not warn about an `@Override` annotation if the parent class also has a method of the same name through a different inheritance path. A test that asserts the secondary index CFS is empty after deleting indexed rows would catch this immediately.

---

## Test
`SecondaryIndexTest` adds assertions that check the index column family store is empty after rows with indexed values are deleted and overwritten, directly verifying that stale entries are purged.
