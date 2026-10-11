# Bug 1547: Fix Native Protocol Drop User Type Notification

**Commit**: 33719e759a · **Ticket**: CASSANDRA-7571
**Subsystem**: src/java/org/apache/cassandra/db/DefsTables.java
**Category**: Wrong constant / default value

---

## Root Cause

When a user-defined type (UDT) was dropped, `DefsTables.dropUserType()` called `MigrationManager.instance.notifyUpdateUserType(ut)` instead of `notifyDropUserType(ut)`. This caused connected clients to receive an `UPDATE` schema change notification for the type rather than a `DROP` notification, meaning clients would not remove the type from their schema cache.

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
