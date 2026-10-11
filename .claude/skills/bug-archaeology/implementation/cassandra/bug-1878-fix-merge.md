# Bug 1878: Fix Merge — Wrong Class Reference in StorageService (join path)

**Commit**: 6bcf28edcb · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Wrong constant / default value

---

## Root Cause

A second merge artifact in `StorageService`: the node-join initialization path called `SystemTable.getLocalHostId()` instead of the renamed `SystemKeyspace.getLocalHostId()`. This would cause a compile-time or runtime failure when the node attempted to seed its host ID into token metadata at startup.

### Before / After

```java
// BEFORE – stale class name
getTokenMetadata().updateHostId(SystemTable.getLocalHostId(), FBUtilities.getBroadcastAddress());
```

```java
// AFTER – correct renamed class
getTokenMetadata().updateHostId(SystemKeyspace.getLocalHostId(), FBUtilities.getBroadcastAddress());
```

---

## How to Spot

After a class rename refactor, search the entire codebase for the old name before merging. A global find-and-replace or IDE refactor tool would have caught all three occurrences atomically.

---

## Test

No test added.
