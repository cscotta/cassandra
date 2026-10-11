**Commit**: c70b7c4b9e · **Ticket**: KAFKA-18323
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Implements a memory model for representing streams groups in the group coordinator, as well as group count and rebalance metrics. Reviewers: Bill Bejeck <bill@confluent.io>, Bruno Cadonna <bruno@confluent.io>

**Before/After**
```java
// Before
              files="(RecordHelpersTest|GroupCoordinatorRecordHelpers|GroupMetadataManager|GroupMetadataManagerTest|OffsetMetadataManagerTest|GroupCoordinatorServiceTest|GroupCoordinatorShardTest|GroupCoordinatorRecordSerde).java"/>

// After
              files="(RecordHelpersTest|GroupCoordinatorRecordHelpers|GroupMetadataManager|GroupMetadataManagerTest|OffsetMetadataManagerTest|GroupCoordinatorServiceTest|GroupCoordinatorShardTest|GroupCoordinatorRecordSerde|StreamsGroupTest).java"/>
        STREAMS("streams"),
import org.apache.kafka.coordinator.group.streams.StreamsGroup.StreamsGroupState;
    public static final String STREAMS_GROUP_COUNT_METRIC_NAME = "streams-group-count";
    public static final String STREAMS_GROUP_COUNT_STATE_TAG = "state";
    public static final String STREAMS_GROUP_REBALANCES_SENSOR_NAME = "StreamsGroupRebalances";
    private final MetricName streamsGroupCountMetricName;
    private final MetricName streamsGroupCountEmptyMetricName;
    private final MetricName streamsGroupCountAssigningMetricName;
    private final MetricName streamsGroupCountReconcilingMetricName;
    private final MetricName streamsGroupCountStableMetricName;
    private final MetricName streamsGroupCountDeadMetricName;
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18323: add streamsgroup class (#18729).

**Test**
Test modified in GroupCoordinatorMetricsTest.java covering the corrected behaviour.
