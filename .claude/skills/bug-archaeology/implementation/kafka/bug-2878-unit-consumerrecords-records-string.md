**Commit**: dd6fcc650e · **Ticket**: KAFKA-16901
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import static org.junit.jupiter.api.Assertions.assertEquals;

    public void iterator() throws Exception {
        Map<TopicPartition, List<ConsumerRecord<Integer, String>>> records = new LinkedHashMap<>();
        String topic = "topic";
        records.put(new TopicPartition(topic, 0), new ArrayList<>());
        ConsumerRecord<Integer, String> record1 = new ConsumerRecord<>(topic, 1, 0, 0L, TimestampType.CREATE_TIME,
            0, 0, 1, "value1", new RecordHeaders(), Optional.empty());
        ConsumerRecord<Integer, String> record2 = new ConsumerRecord<>(topic, 1, 1, 0L, TimestampType.CREATE_TIME,
            0, 0, 2, "value2", new RecordHeaders(), Optional.empty());
        records.put(new TopicPartition(topic, 1), Arrays.asList(record1, record2));
        records.put(new TopicPartition(topic, 2), new ArrayList<>());

// After
import java.util.Collection;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

    public void testIterator() {
        String topic = "topic";
        int recordSize = 10;
        int partitionSize = 15;
        int emptyPartitionIndex = 3;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16901 add unit tests for consumerrecords#records(string) (#16227).

**Test**
Test modified in ConsumerRecordsTest.java covering the corrected behaviour.
