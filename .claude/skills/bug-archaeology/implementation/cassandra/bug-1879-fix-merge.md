# Bug 1879: Fix Merge — Wrong Class Reference in StorageService (replace-address setup)

**Commit**: 4dc5d690f1 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Wrong constant / default value

---

## Root Cause

A third merge artifact in `StorageService`: the replace-address setup path called `SystemTable.setLocalHostId(hostId)` instead of the renamed `SystemKeyspace.setLocalHostId(hostId)`. This would prevent a replacing node from persisting the replacee's host ID, breaking hint delivery and token ownership during node replacement.

### Before / After

```java
// BEFORE – stale class name
SystemTable.setLocalHostId(hostId); // use the replacee's host Id as our own so we receive hints, etc
```

```java
// AFTER – correct renamed class
SystemKeyspace.setLocalHostId(hostId); // use the replacee's host Id as our own so we receive hints, etc
```

---

## How to Spot

After a class rename refactor, search the entire codebase for the old name before merging. A global find-and-replace or IDE refactor tool would have caught all three occurrences atomically.

---

## Test

No test added.
