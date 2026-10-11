**Commit**: ee834d9214 · **Ticket**: KAFKA-15045
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR updates all of the streams task assignment code to use the new AssignmentConfigs public class. Reviewers: Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
                             final OptionalInt  rackAwareNonOverlapCost,
            rackAwareTrafficCost
            rackAwareNonOverlapCost
import org.apache.kafka.str

// After
                             final OptionalInt rackAwareNonOverlapCost,
            defaultRackAwareTrafficCost(rackAwareTrafficCost)
            defaultRackAwareNonOverlapCost(rackAwareNonOverlapCost)
    public AssignmentConfigs(final long acceptableRecoveryLag,
                             final int maxWarmupReplicas,
                             final int numStandbyReplicas,
                             final long probingRebalanceIntervalMs,
                             final List<String> rackAwareAssignmentTags,
                             final int rackAwareTrafficCost,
                             final int rackAwareNonOverlapCost,
                             final String rackAwareAssignmentStrategy) {
        this(acceptableRecoveryLag, maxWarmupReplicas, numStandbyReplicas, probingRebalanceIntervalMs, rackAwareAssignmentTags,
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15045: (kip-924 pt. 19) update to new assignmentconfigs (#16219).

**Test**
Test modified in TaskAssignorIntegrationTest.java covering the corrected behaviour.
