# Bug 0272: Schema Reset Leaves Stale Version Info in MigrationCoordinator

**Commit**: 76be530a36 · **Ticket**: CASSANDRA-17819
**Subsystem**: schema/MigrationCoordinator
**Category**: State not cleaned up

---

## Root Cause

`MigrationCoordinator.reset()` only cleared `versionInfo` but left `endpointVersions` intact.
After a reset, previously-known endpoint-to-version mappings remained in `endpointVersions`, so
when live peers re-advertised their schema versions the coordinator compared against stale entries
and concluded nothing had changed, silently skipping schema re-fetch.  Additionally, waiting
threads blocked on `VersionInfo.waitQueue` were never signalled, leaving them suspended
indefinitely after the reset.

### Before / After

```java
// BEFORE
public synchronized void reset()
{
    versionInfo.clear();
}
```

```java
// AFTER
public synchronized void reset()
{
    this.endpointVersions.clear();
    clearVersionsInfo(); // signals all waiters, then removes all entries
}

private synchronized void clearVersionsInfo()
{
    Iterator<Map.Entry<UUID, VersionInfo>> it = versionInfo.entrySet().iterator();
    while (it.hasNext())
    {
        Map.Entry<UUID, VersionInfo> entry = it.next();
        it.remove();
        entry.getValue().waitQueue.signalAll();
    }
}
```

---

## How to Spot

Whenever a "reset" or "clear" operation touches one of several coupled data structures, verify
that every related structure is also cleared and that any threads waiting on invalidated state
are unblocked.  In this case `versionInfo` and `endpointVersions` are kept in sync by every
other mutation method (`reportEndpointVersion`, `removeEndpointFromVersion`), so missing one in
`reset()` breaks that invariant.  A useful review heuristic: search for all field names in the
class that are modified by the same mutation methods and confirm the reset path touches them all.

---

## Test

`SchemaTest#schemaReset` verifies that after `resetLocalSchema()` is called on a node with no
live peers the schema is empty, and that after the peer restarts the schema is re-acquired within
a configurable pull interval.
