# Bug 2180: initializeNodeUnsafe Ignores Existing Endpoint State After putIfAbsent

**Commit**: 792bd184f5 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/gms/Gossiper.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`initializeNodeUnsafe` created a new `EndpointState`, inserted it with `putIfAbsent`, but then always modified the locally constructed `localState` variable regardless of whether the put actually succeeded. If the endpoint was already present in `endpointStateMap`, `putIfAbsent` would return the existing state and leave the map unchanged, but the subsequent `addApplicationState` calls would still write to the newly created (and immediately discarded) `newState` object. The fix captures the return value of `putIfAbsent`: if it is non-null (an existing entry was present), the existing state is used for subsequent mutations.

### Before / After

```java
// BEFORE
EndpointState localState = new EndpointState(hbState);
localState.markAlive();
endpointStateMap.putIfAbsent(addr, localState);
// localState is always the new object, even if the put was a no-op
```

```java
// AFTER
EndpointState newState = new EndpointState(hbState);
newState.markAlive();
EndpointState oldState = endpointStateMap.putIfAbsent(addr, newState);
EndpointState localState = oldState == null ? newState : oldState;
```

---

## How to Spot

Any time `putIfAbsent` (or similar conditional-insert operations) is called without checking its return value, the subsequent code may be operating on the wrong object. Always capture and branch on the return value of `putIfAbsent`.

---

## Test

No test added.
