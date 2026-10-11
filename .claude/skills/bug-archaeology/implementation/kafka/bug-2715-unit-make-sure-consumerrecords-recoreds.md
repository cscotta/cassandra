**Commit**: 43235c2796 · **Ticket**: KAFKA-17133
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: TingIāu "Ting" Kì <kitingiao@gmail.com>, Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before


// After
    @Test
    public void testRecordsAreImmutable() {
        String topic = "topic";
        int recordSize = 3;
        int partitionSize = 6;
        int emptyPartitionIndex = 2;
        TopicPartition topicPartition = new TopicPartition(topic, 0);
        ConsumerRecord<Integer, String> newRecord = new ConsumerRecord<>(topic, 0, 0, 0L, TimestampType.CREATE_TIME,
            0, 0, 0, "0", new RecordHeaders(), Optional.empty());
        ConsumerRecords<Integer, String> records = buildTopicTestRecords(recordSize, partitionSize, emptyPartitionIndex, Collections.singleton(topic));
        ConsumerRecords<Integer, String> emptyRecords = ConsumerRecords.empty();

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17133  add unit test to make sure `consumerrecords#recoreds` returns immutable object (#16588).

**Test**
Test modified in ConsumerRecordsTest.java covering the corrected behaviour.
