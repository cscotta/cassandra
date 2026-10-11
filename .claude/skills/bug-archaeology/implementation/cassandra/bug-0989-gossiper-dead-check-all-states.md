# Bug 0989: Gossiper Dead-State Check Runs for Every ApplicationState Change

**Commit**: 73793d6f0e · **Ticket**: n/a
**Subsystem**: Gossip / Snitch
**Category**: Logic error in condition

---

## Root Cause
`ReconnectableSnitchHelper.onChange` evaluated `!Gossiper.instance.isDeadState(...)` before checking `state == ApplicationState.INTERNAL_IP`. Because `isDeadState` does a map lookup on the gossip state machine, it ran on every single `ApplicationState` change broadcast — the vast majority of which are irrelevant to this helper. Moving the cheap enum comparison first means the expensive gossip lookup is only performed when the event is actually an `INTERNAL_IP` update.

### Before / After
```java
// BEFORE — dead check evaluated for all ApplicationState values
if (preferLocal && !Gossiper.instance.isDeadState(Gossiper.instance.getEndpointStateForEndpoint(endpoint)) && state == ApplicationState.INTERNAL_IP)

// AFTER — cheap state check first (short-circuit)
if (preferLocal && state == ApplicationState.INTERNAL_IP && !Gossiper.instance.isDeadState(Gossiper.instance.getEndpointStateForEndpoint(endpoint)))
```

---

## How to Spot
In gossip `onChange` callbacks, expensive lookups (hash-map reads, lock acquisitions, JMX operations) should always be guarded by a cheap `state ==` comparison first. Review every `IEndpointStateChangeSubscriber.onChange` implementation and confirm the `ApplicationState` filter comes before any non-trivial side effects.

---

## Test
No test added.
