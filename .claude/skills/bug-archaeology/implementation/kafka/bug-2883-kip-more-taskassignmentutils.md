**Commit**: 16e2b68b73 · **Ticket**: KAFKA-15045
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Also moved the assignment validation test from StreamsPartitionAssignorTest to TaskAssignmentUtilsTest. Reviewers: Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
import org.apache.kafka.streams.processor.assignment.assignors.StickyTaskAssignor;
import org.apache.kafka.streams.processor.internals.assignment.HighAvailabilityTaskAssignor;
        Integer rackAwareTrafficCost = configs.getInt(StreamsConfig.RACK_AWARE_ASSIGNMENT_TRAFFIC_COST_CONFIG);
        Integer rackAwareNonOverlapCost = configs.getInt(StreamsConfig.RACK_AWARE_ASSIGNMENT_NON_OVERLAP_COST_CONFIG);

        final String assignorClassName = configs.getString(StreamsConfig.TASK_ASSIGNOR_CLASS_CONFIG);
        if (StickyTaskAssignor.class.getName().equals(assignorClassName)) {
            if (rackAwareTrafficCost == null) {
                rackAwareTrafficCost = StickyTaskAssignor.DEFAULT_STICKY_TRAFFIC_COST;
            }
            if (rackAwareNonOverlapCost == null) {
                rackAwareNonOverlapCost = StickyTaskAssignor.DEFAULT_STICKY_NON_OVERLAP_COST;

// After
        final Integer rackAwareTrafficCost = configs.getInt(StreamsConfig.RACK_AWARE_ASSIGNMENT_TRAFFIC_COST_CONFIG);
        final Integer rackAwareNonOverlapCost = configs.getInt(StreamsConfig.RACK_AWARE_ASSIGNMENT_NON_OVERLAP_COST_CONFIG);
import java.util.Arrays;
    @Override
    public String toString() {
        return String.format(
            "KafkaStreamsAssignment{%s, %s, %s}",
            processId,
            Arrays.toString(tasks.values().toArray(new AssignedTask[0])),
            followupRebalanceDeadline
        );
    }
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15045: (kip-924 pt. 23) more taskassignmentutils tests (#16292).

**Test**
Test modified in StreamsPartitionAssignorTest.java covering the corrected behaviour.
