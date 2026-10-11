# Bug 1980: UP / join-cluster lifecycle events silently dropped during normal token state transitions

**Commit**: 27efded38d · **Ticket**: CASSANDRA-5769
**Subsystem**: service/StorageService.java
**Category**: Silent skip on non-match

---

## Root Cause

`IEndpointLifecycleSubscriber.onUp()` and `onJoinCluster()` were called inside `handleStateNormal()`, the gossip handler that fires when a node's token state becomes NORMAL. However, `handleStateNormal()` is also called when a node re-asserts its already-normal state (e.g. after restart), so the UP event could fire before the node was actually alive. The real signal for a node coming up is `onAlive()` in the failure-detector callback. The event dispatch was moved to `onAlive()`, which fires exactly when the endpoint becomes reachable. This ensures native-protocol clients always receive the UP event from the correct lifecycle phase.

### Before / After

```java
// BEFORE: UP events dispatched in handleStateNormal, missing nodes that don't trigger re-gossip
if (tokenMetadata.isMember(endpoint)) {
    logger.info("Node " + endpoint + " state jump to normal");
    if (!isClientMode) {
        for (IEndpointLifecycleSubscriber subscriber : lifecycleSubscribers)
            subscriber.onUp(endpoint);
    }
} else if (!isClientMode) {
    for (IEndpointLifecycleSubscriber subscriber : lifecycleSubscribers)
        subscriber.onJoinCluster(endpoint);
}
```

```java
// AFTER: events dispatched in onAlive(), the correct liveness callback
public void onAlive(InetAddress endpoint, EndpointState state) {
    if (isClientMode) return;
    if (tokenMetadata.isMember(endpoint)) {
        HintedHandOffManager.instance.scheduleHintDelivery(endpoint);
        for (IEndpointLifecycleSubscriber subscriber : lifecycleSubscribers)
            subscriber.onUp(endpoint);
    } else {
        for (IEndpointLifecycleSubscriber subscriber : lifecycleSubscribers)
            subscriber.onJoinCluster(endpoint);
    }
}
```

---

## How to Spot

When notifying external observers about node liveness, verify that the notification is tied to the actual liveness event (`onAlive`) rather than to a state-machine transition (`handleStateNormal`) that may fire at times unrelated to the node becoming reachable.

---

## Test

No test added.
