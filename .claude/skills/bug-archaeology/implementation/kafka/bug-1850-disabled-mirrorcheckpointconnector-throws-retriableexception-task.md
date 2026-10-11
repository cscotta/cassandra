**Commit**: de2ccb5789 · **Ticket**: KAFKA-18021
**Subsystem**: connect/mirror
**Category**: Logic error in condition

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
        // if the replication is disabled, known consumer group is empty, or checkpoint emission is
        // disabled by setting 'emit.checkpoints.enabled' to false, the interval of checkpoint emission
        // will be negative and no 'MirrorCheckpointTask' will be created
        if (!config.enabled() || knownConsumerGroups.isEmpty()
                || config.emitCheckpointsInterval().isNegative()) {

// After
        // If the replication is disabled or checkpoint emission is disabled by setting 'emit.checkpoints.enabled' to false,
        // the interval of checkpoint emission will be negative and no 'MirrorCheckpointTask' will be created.
        if (!config.enabled() || config.emitCheckpointsInterval().isNegative()) {
            return Collections.emptyList();
        }

        // If the consumer group is empty, no 'MirrorCheckpointTask' will be created.
        if (knownConsumerGroups.isEmpty()) {

```

**How to spot**
When reviewing connect/mirror code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18021: disabled mirrorcheckpointconnector throws retriableexception on task config generation (#18098).

**Test**
No test added.
