# Bug 1579: Fix replace_address After CASSANDRA-7356 Broke It

**Commit**: d84b692eb1 · **Ticket**: CASSANDRA-7356
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java · src/java/org/apache/cassandra/config/DatabaseDescriptor.java
**Category**: Logic error in condition

---

## Root Cause

A previous refactor (CASSANDRA-7356) moved the bootstrap-completion guard (`SystemTable.bootstrapComplete()` check) into `DatabaseDescriptor.isReplacing()`. This was too early in the call chain: `isReplacing()` is also called before the node has had a chance to run through the replacement flow in `StorageService`, so a node that had ever completed bootstrap would always fail with "Cannot replace address with a node that is already bootstrapped" even when it legitimately should be acting as a replacement node. The fix removes the guard from `DatabaseDescriptor` and moves it back into `StorageService.joinTokenRing()`, where the replacement context is established.

### Before / After

```java
// BEFORE – guard in DatabaseDescriptor.isReplacing() fires too early
if (getReplaceAddress() != null && SystemTable.bootstrapComplete())
    throw new RuntimeException("Cannot replace address with a node that is already bootstrapped");
return getReplaceAddress() != null;
```

```java
// AFTER – guard moved to StorageService where replacement context is known
if (DatabaseDescriptor.isReplacing())
{
    if (SystemTable.bootstrapComplete())
        throw new RuntimeException("Cannot replace address with a node that is already bootstrapped");
    ...
}
```

---

## How to Spot

When moving a guard condition during a refactor, verify that the new location is still reached only in the same contexts as before. A guard placed in a low-level utility method will fire for every caller, not just the intended one.

---

## Test

No test added.
