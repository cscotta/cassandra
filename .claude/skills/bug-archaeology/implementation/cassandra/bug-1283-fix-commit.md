# Bug 1283: ReconnectableSnitchHelper Uses Undefined `epState` Variable in `onChange`

**Commit**: 738229bd79 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/locator/ReconnectableSnitchHelper.java
**Category**: Logic error in condition

---

## Root Cause

The `onChange` method received an `endpoint` argument but called `Gossiper.instance.isDeadState(epState)` using an `epState` variable that does not exist in its scope — `epState` is only a parameter name in the sibling `onJoin` method. This would cause a compile error or, if it somehow compiled, would reference a stale or wrong endpoint state object. The correct call is to look up the current state for the specific endpoint being changed: `Gossiper.instance.getEndpointStateForEndpoint(endpoint)`. As a result, the dead-state guard in `onChange` was either broken or checking the wrong node's liveness, potentially triggering reconnects to dead endpoints.

### Before / After

```java
// BEFORE — epState is not defined in this method's scope
public void onChange(InetAddress endpoint, ApplicationState state, VersionedValue value)
{
    if (preferLocal && !Gossiper.instance.isDeadState(epState) && state == ApplicationState.INTERNAL_IP)
        reconnect(endpoint, value);
}
```

```java
// AFTER — look up the correct endpoint state dynamically
public void onChange(InetAddress endpoint, ApplicationState state, VersionedValue value)
{
    if (preferLocal && !Gossiper.instance.isDeadState(Gossiper.instance.getEndpointStateForEndpoint(endpoint)) && state == ApplicationState.INTERNAL_IP)
        reconnect(endpoint, value);
}
```

---

## How to Spot

When multiple event-handler methods in the same class share similar guard conditions, review each method's parameter list independently rather than assuming that variables used in one handler are available in another. A copy-paste from `onJoin(InetAddress, EndpointState epState)` into `onChange(InetAddress, ApplicationState, VersionedValue)` silently transfers a parameter name that does not exist in the new signature.

---

## Test

No test added.
