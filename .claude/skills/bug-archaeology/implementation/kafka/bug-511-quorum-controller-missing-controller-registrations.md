## Bug 511 — QuorumController: isNodeIdRegistered missed controllerRegistrations

**Commit:** 45a51fe19e
**Ticket:** (dynamic quorum fix — no standalone JIRA cited)
**Subsystem:** controller / KRaft
**Category:** Logic error in condition

### Root cause
`QuorumController.isNodeIdRegistered` checked only `brokerRegistrations` and `featureControl.isControllerId`, missing the `controllerRegistrations` map. In a dynamic KRaft quorum where controllers register separately, a controller node that had registered but was not yet the active controller would not be found, causing the controller to incorrectly conclude the node was unregistered and potentially reject or mis-route operations.

### Before
```java
boolean isNodeIdRegistered(int nodeId) {
    return brokerRegistrations.containsKey(nodeId)
        || featureControl.isControllerId(nodeId);
}
```

### After
```java
boolean isNodeIdRegistered(int nodeId) {
    return brokerRegistrations.containsKey(nodeId)
        || controllerRegistrations.containsKey(nodeId)
        || featureControl.isControllerId(nodeId);
}
```

### How to spot
Any predicate that checks membership across multiple registration maps must include all maps. In KRaft, broker and controller nodes are stored in separate maps; a check limited to one is incomplete.

### Test
`QuorumControllerTest` extended with a scenario where a controller-only node is registered and `isNodeIdRegistered` must return `true`.
