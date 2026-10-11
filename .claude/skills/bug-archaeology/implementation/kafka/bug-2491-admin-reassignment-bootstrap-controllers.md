**Commit**: 5557720246 · **Ticket**: KAFKA-17038
**Subsystem**: clients/admin/KafkaAdminClient
**Category**: Wrong constant / default value

**Root cause**
`KafkaAdminClient.alterPartitionReassignments()` and `listPartitionReassignments()` constructed their `ControllerNodeProvider` without the `useBootstrapControllers=true` flag. As a result, these operations used the broker-based metadata channel and ignored `bootstrap.controllers` configuration, making it impossible for clients connecting directly to KRaft controllers to issue reassignment requests. The fix passes `true` to the `ControllerNodeProvider` constructor so the controller channel is used when `BOOTSTRAP_CONTROLLERS_CONFIG` is set.

**Before/After**
```java
// Before: useBootstrapControllers defaulted to false
new ControllerNodeProvider(metadataManager, ...)

// After
new ControllerNodeProvider(metadataManager, ..., /* useBootstrapControllers= */ true)
```

**How to spot**
After adding a `useBootstrapControllers` flag to `ControllerNodeProvider`, audit all call sites that construct it and verify the flag is set appropriately for each admin operation. Operations that target the controller (reassignments, elections, metadata updates) need the flag set to `true`; broker-targeted operations do not.

**Test**
`KafkaAdminClientTest` updated to verify that reassignment calls use the controller channel when `bootstrap.controllers` is configured.
