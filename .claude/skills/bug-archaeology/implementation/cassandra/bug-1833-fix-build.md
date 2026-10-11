# Bug 1833: Fix Build — Remove Orphaned unregister() Call in Table.java

**Commit**: cc8a05ab6a · **Ticket**: CASSANDRA-6345
**Subsystem**: src/java/org/apache/cassandra/db
**Category**: Wrong constant / default value

---

## Root Cause

The thundering-herd refactor (commit 8145c83566) removed the subscriber/observer mechanism from `TokenMetadata` — the `register()` and `unregister()` methods were deleted. `Table.createReplicationStrategy()` still contained a call to `tokenMetadata.unregister(replicationStrategy)` that referenced the now-deleted method, breaking compilation. The fix is to simply remove the dead call.

### Before / After

```java
// BEFORE (Table.java)
public void createReplicationStrategy(KSMetaData ksm)
{
    if (replicationStrategy != null)
        StorageService.instance.getTokenMetadata().unregister(replicationStrategy);

    replicationStrategy = AbstractReplicationStrategy.createReplicationStrategy(...);
}
```

```java
// AFTER (Table.java)
public void createReplicationStrategy(KSMetaData ksm)
{
    replicationStrategy = AbstractReplicationStrategy.createReplicationStrategy(...);
}
```

---

## How to Spot

After deleting a method from a class, verify that no callers remain in any file — particularly in sibling subsystems that interact with the changed class (here, `Table` interacting with `TokenMetadata`).

---

## Test

No test added.
