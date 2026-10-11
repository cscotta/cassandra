# Bug 0543: Gossip Fires Change Notifications for Already-Current States

**Commit**: 3f70e7c72c · **Ticket**: CASSANDRA-15097
**Subsystem**: `src/java/org/apache/cassandra/gms/Gossiper.java`
**Category**: Logic error in condition

---

## Root Cause

In `Gossiper.applyStateLocally`, when a remote endpoint state's generation matched the local generation the code called `localState.addApplicationStates(remoteStates)` unconditionally, then fired `doOnChangeNotifications` for every remote state entry regardless of whether the local node already held an equal or newer version of that state. Subscribers such as `StorageService` would then re-process topology changes (e.g., token updates) that had already been applied, causing spurious work and potential inconsistencies. The fix filters `remoteStates` down to entries whose version number exceeds the locally stored version before updating and notifying.

### Before / After

```java
// BEFORE – applies and notifies all remote states unconditionally
localState.addApplicationStates(remoteStates);
for (Entry<ApplicationState, VersionedValue> remoteEntry : remoteStates)
    doOnChangeNotifications(addr, remoteEntry.getKey(), remoteEntry.getValue());
```

```java
// AFTER – only propagate states with a newer version
Set<Entry<ApplicationState, VersionedValue>> updatedStates = remoteStates.stream()
    .filter(entry -> {
        VersionedValue local = localState.getApplicationState(entry.getKey());
        return (local == null || local.version < entry.getValue().version);
    }).collect(Collectors.toSet());
localState.addApplicationStates(updatedStates);
for (Entry<ApplicationState, VersionedValue> updatedEntry : updatedStates)
    doOnChangeNotifications(addr, updatedEntry.getKey(), updatedEntry.getValue());
```

---

## How to Spot

Gossip and other eventually-consistent state-merge paths should always use versioned values as idempotency guards: before updating local state and firing notifications, check that the incoming version is strictly greater than what is already stored. Any merge loop that applies and notifies without first comparing versions is susceptible to duplicate notifications, which can cascade into redundant work or incorrect state machine transitions in subscribers.

---

## Test

Added `testDuplicatedStateUpdate` which applies the same `TOKENS` versioned value twice (bumping only the heartbeat between applications) and asserts that `onChange` fires exactly once, not twice.
