# Bug 1550: Fix Native Protocol Drop User Type Notification

**Commit**: 82fa5d5077 · **Ticket**: CASSANDRA-7571
**Subsystem**: src/java/org/apache/cassandra/db/DefsTables.java
**Category**: Wrong constant / default value

---

## Root Cause

Same wrong-notification bug as commit 33719e759a, applied to a different branch. When a user-defined type was dropped, `DefsTables.dropUserType()` incorrectly called `notifyUpdateUserType(ut)` instead of `notifyDropUserType(ut)`, causing connected clients to receive an `UPDATE` event instead of a `DROP` event, leaving stale type definitions in client schema caches.

### Before / After

```java
// BEFORE
if (!StorageService.instance.isClientMode())
    MigrationManager.instance.notifyUpdateUserType(ut);
```

```java
// AFTER
if (!StorageService.instance.isClientMode())
    MigrationManager.instance.notifyDropUserType(ut);
```

---

## How to Spot

When implementing drop/update/create schema operations, ensure the notification method called precisely matches the operation type. Copy-paste errors between `notifyUpdate*` and `notifyDrop*` are easy to miss since both compile and run without errors, but produce wrong schema events on the wire.

---

## Test

No test added.
