# Bug 1032: NPE in Gossip handleStateNormal due to Race on EndpointState ApplicationState Map

**Commit**: 6bad57fc3c · **Ticket**: CASSANDRA-10089
**Subsystem**: Gossip / Cluster Membership
**Category**: Race condition / missing synchronization

---

## Root Cause
`EndpointState` stored its application-state map in a `NonBlockingHashMap`, a lock-free structure that allows concurrent reads and writes. Code paths in `handleStateNormal` and `getTokensFor` called `getApplicationState(TOKENS)` or iterated the map while another thread could insert or replace entries, leading to a NPE when the map returned `null` for a key that was assumed to be present. The core issue was that two-field updates (e.g., `STATUS` + `REMOVAL_COORDINATOR`) were not atomic: a reader could observe a partially-applied state.

The fix replaces the `NonBlockingHashMap` with an `AtomicReference<EnumMap>` and a copy-on-write update loop (`addApplicationStates`). Every multi-field write now produces a fresh snapshot and swings the reference atomically, so readers always see a consistent, complete state. Additionally, `getTokensFor` was hardened to null-check both the `EndpointState` and the `VersionedValue` before dereferencing.

### Before / After
```java
// BEFORE – single-state updates, non-atomic; reader may see partial state
epState.addApplicationState(ApplicationState.STATUS, valueFactory.removingNonlocal(hostId));
epState.addApplicationState(ApplicationState.REMOVAL_COORDINATOR, valueFactory.removalCoordinator(localHostId));

// BEFORE – getTokensFor unconditionally dereferences, NPE if null
Collection<Token> tokens = getTokensFor(endpoint); // crashes when state absent

// AFTER – atomic batch update via copy-on-write EnumMap
Map<ApplicationState, VersionedValue> states = new EnumMap<>(ApplicationState.class);
states.put(ApplicationState.STATUS, valueFactory.removingNonlocal(hostId));
states.put(ApplicationState.REMOVAL_COORDINATOR, valueFactory.removalCoordinator(localHostId));
epState.addApplicationStates(states);

// AFTER – explicit null guards in getTokensFor
EndpointState state = Gossiper.instance.getEndpointStateForEndpoint(endpoint);
if (state == null) return Collections.emptyList();
VersionedValue versionedValue = state.getApplicationState(ApplicationState.TOKENS);
if (versionedValue == null) return Collections.emptyList();
```

---

## How to Spot
Whenever code reads a `VersionedValue` out of `EndpointState` and immediately dereferences it without a null check, it is vulnerable to this pattern — especially if the write path uses separate single-entry `addApplicationState` calls that are logically related. In review, look for any place that calls `getApplicationState(X).value` or passes the result to a method without an intervening null guard, and check whether the corresponding writer always guarantees the key is present before gossip events are processed.

---

## Test
`EndpointStateTest` was added to exercise the copy-on-write `addApplicationStates` logic and verify that concurrent readers see only fully-applied state snapshots.
