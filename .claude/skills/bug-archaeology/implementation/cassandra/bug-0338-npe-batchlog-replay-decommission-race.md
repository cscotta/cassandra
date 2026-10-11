# Bug 0338: NPE in Batchlog Replay During Node Decommission Race

**Commit**: 58c878e3e0 · **Ticket**: CASSANDRA-17049
**Subsystem**: src/java/org/apache/cassandra/batchlog/BatchlogManager.java
**Category**: Missing null / bounds check

---

## Root Cause

`BatchlogManager` tracked "hinted nodes" as a `Set<InetAddress>`. When writing hints for undelivered endpoints it called `StorageService.instance.getHostIdForEndpoint(address)` and passed the result directly to `HintsService.instance.write()` without checking for null. During node decommission the host ID mapping can temporarily disappear, causing `getHostIdForEndpoint` to return null and triggering an NPE inside the hints subsystem. The fix changes the tracking set to `Set<UUID>` (host IDs rather than addresses), adds a null guard around every call to `getHostIdForEndpoint`, and skips hinting if the ID is unavailable. A parallel bug in `HintsService.flushAndFsyncBlockingly` passed host IDs through `catalog::get` which could also return null; this was fixed by using `catalog::getNullable` and filtering out null stores.

### Before / After

```java
// BEFORE – no null check; NPE if node is decommissioning
hintedNodes.add(endpoint);
HintsService.instance.write(StorageService.instance.getHostIdForEndpoint(endpoint),
                            Hint.create(mutation, writtenAt));
```

```java
// AFTER – null-safe
UUID hostId = StorageService.instance.getHostIdForEndpoint(endpoint);
if (null != hostId)
{
    HintsService.instance.write(hostId, Hint.create(mutation, writtenAt));
    hintedNodes.add(hostId);
}
```

---

## How to Spot

Any call to a lookup that maps a live address to a stable identifier (like `getHostIdForEndpoint`) should be treated as nullable: membership changes (decommission, replace, bootstrap) can temporarily break the mapping. Review every call site for null handling, especially in background tasks like batchlog replay that run concurrently with topology changes.

---

## Test

No new test added; the fix is a defensive null-guard in a hard-to-reproduce concurrent scenario.
