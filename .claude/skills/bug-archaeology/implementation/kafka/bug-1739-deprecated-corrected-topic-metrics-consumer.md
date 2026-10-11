**Commit**: f88cf57e1c · **Ticket**: KAFKA-12469
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
The PR implements the behaviour defined in KIP-1109. It corrects the consumer topic and topic-partition metrics, while deprecating the incorrect ones. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Jun Rao <junrao@gmail.com>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
import java.util.HashMap;
        Sensor bytesFetched = new SensorBuilder(metrics, name, () -> topicTags(topic))
                .withAvg(metricsRegistry.topicFetchSizeAvg)
                .withMax(metricsRegistry.topicFetchSizeMax)
                .withMeter(metricsRegistry.topicBytesConsumedRate, metricsRegistry.topicBytesConsumedTotal)
                .build();
        Sensor recordsFetched = new SensorBuilder(metrics, name, () -> topicTags(topic))
                .withAvg(metricsRegistry.topicRecordsPerRequestAvg)
                .withMeter(metricsRegistry.topicRecordsConsumedRate, metricsRegistry.topicRecordsConsumedTotal)
                .build();
        Sensor recordsLag = new SensorBuilder(metrics, name, () -> topicPartitionTags(tp))
                .withValue(metricsRegistry.partitionRecordsLag)

// After
import static org.apache.kafka.common.utils.Utils.mkEntry;
import static org.apache.kafka.common.utils.Utils.mkMap;

        maybeRecordDeprecatedBytesFetched(name, topic, bytes);

        Sensor bytesFetched = new SensorBuilder(metrics, name, () -> Map.of("topic", topic))
            .withAvg(metricsRegistry.topicFetchSizeAvg)
            .withMax(metricsRegistry.topicFetchSizeMax)
            .withMeter(metricsRegistry.topicBytesConsumedRate, metricsRegistry.topicBytesConsumedTotal)
            .build();
        maybeRecordDeprecatedRecordsFetched(name, topic, records);

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12469: deprecated and corrected topic metrics for consumer (kip-1109) (#18232).

**Test**
Test modified in FetchMetricsManagerTest.java covering the corrected behaviour.
