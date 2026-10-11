**Commit**: 5ce4b91dfa · **Ticket**: KAFKA-15045
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR takes care of making the call back toTaskAssignor.onAssignmentComputed. It also contains a change to the public AssignmentConfigs API, as well as some simplifications of the StickyTaskAssignor. This PR also changes the rack information fetching to happen lazily in the case where the TaskAssignor makes its decisions without said rack information. Reviewers: Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
     * @return the set of all tasks in this topology which must be assigned
    private final int rackAwareTrafficCost;
    private final int rackAwareNonOverlapCost;
    public AssignmentConfigs(final StreamsConfig configs) {
        this(
            configs.getLong(StreamsConfig.ACCEPTABLE_RECOVERY_LAG_CONFIG),
            configs.getInt(StreamsConfig.MAX_WARMUP_REPLICAS_CONFIG),
            configs.getInt(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG),
            configs.getLong(StreamsConfig.PROBING_REBALANCE_INTERVAL_MS_CONFIG),
            configs.getList(StreamsConfig.RACK_AWARE_ASSIGNMENT_TAGS_CONFIG),
            configs.getInt(StreamsConfig.RACK_AWARE_ASSIGNMENT_TRAFFIC_COST_CONFIG),
            configs.getInt(StreamsConfig.RACK_AWARE_ASSIGNMENT_NON_OVERLAP_COST_CONFIG),

// After
     * @return a map of task ids to all tasks in this topology to be assigned
import java.util.OptionalInt;
import org.apache.kafka.streams.processor.assignment.assignors.StickyTaskAssignor;
import org.apache.kafka.streams.processor.internals.assignment.HighAvailabilityTaskAssignor;
    private final OptionalInt rackAwareTrafficCost;
    private final OptionalInt rackAwareNonOverlapCost;
    public static AssignmentConfigs of(final StreamsConfig configs) {
        final long acceptableRecoveryLag = configs.getLong(StreamsConfig.ACCEPTABLE_RECOVERY_LAG_CONFIG);
        final int maxWarmupReplicas = configs.getInt(StreamsConfig.MAX_WARMUP_REPLICAS_CONFIG);
        final int numStandbyReplicas = configs.getInt(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG);
        final long probingRebalanceIntervalMs = configs.getLong(StreamsConfig.PROBING_REBALANCE_INTERVAL_MS_CONFIG);
        final List<String> rackAwareAssignmentTags = configs.getList(StreamsConfig.RACK_AWARE_ASSIGNMENT_TAGS_CONFIG);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15045: (kip-924 pt. 16) taskassignor.onassignmentcomputed handling (#16147).

**Test**
Test modified in TaskAssignorIntegrationTest.java covering the corrected behaviour.
