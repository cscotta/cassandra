**Commit**: 6f783f8536 · **Ticket**: KAFKA-10551
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
- Add support topicId in `ProduceRequest`/`ProduceResponse`. Topic name
and Topic Id will become `ignorable` following the footstep of
`FetchRequest`/`FetchResponse`
- ReplicaManager still look for `HostedPartition` using `TopicPartition`
and doesn't check topic id. This is an **[OPEN QUESTION]** if we should
address this in this pr or wait for
[KAFKA-16212](https://issues.apache.org/jira/browse/KAFKA-16212) as this
will update `ReplicaManager::getPartition` to use `TopicIdParittion`
once we update the cache. Other option is that we compare provided
`topicId` with `Partition` topic id and return `UNKNOW_TOPIC_ID` or
`UNKNOW_TOPIC_PARTITION` if we can't find partition with matched topic
id.

**Before/After**
```java
// Before
                    TopicPartition tp = new TopicPartition(r.name(), p.index());
            ProduceRequestData.TopicProduceData tpData = tpd.find(tp.topic());
                tpData = new ProduceRequestData.TopicProduceData().setName(tp.topic());
import org.apache.kafka.common.TopicPartition;
    private volatile Map<TopicPartition, Integer> partitionSizes;

// After
import org.apache.kafka.common.Uuid;
                    // Version 13 drop topic name and add support to topic id. However, metadata can be used to map topic id to topic name.
                    String topicName = metadata.topicNames().getOrDefault(r.topicId(), r.name());
                    TopicPartition tp = new TopicPartition(topicName, p.index());
        Map<String, Uuid> topicIds = topicIdsForBatches(batches);

            Uuid topicId = topicIds.get(tp.topic());
            ProduceRequestData.TopicProduceData tpData = tpd.find(tp.topic(), topicId);

                tpData = new ProduceRequestData.TopicProduceData()
                        .setTopicId(topicId).setName(tp.topic());

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10551: add topic id support to produce request and response (#15968).

**Test**
Test modified in KafkaProducerTest.java covering the corrected behaviour.
