**Commit**: cbd72cc216 · **Ticket**: KAFKA-14121
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Christo Lolov <lolovc@amazon.com>, Chia-Ping Tsai <chia7712@gmail.com>, TengYao Chi <kitingiao@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
     *   <li>{@link org.apache.kafka.common.errors.InvalidReplicationFactorException}
     *   If the replication factor was changed in an invalid way.
     *   Only thrown when {@link AlterPartitionReassignmentsOptions#allowReplicationFactorChange()} is set to false and
     *   the request is attempting to alter reassignments (not cancel)</li>
     *   <li>{@link org.apache.kafka.common.errors.UnsupportedVersionException}
     *   If {@link AlterPartitionReassignmentsOptions#allowReplicationFactorChange()} was changed outside the default
     *   and the server does not support the option (e.g due to an old Kafka version).</li>

    private boolean allowReplicationFactorChange = true;

    /**
     * Set the option indicating if the alter partition reassignments call should be
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14121: alterpartitionreassignments api should allow callers to specify the option of preserving the replication fa.

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
