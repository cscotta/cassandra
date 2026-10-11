**Commit**: d36eb9ed3f · **Ticket**: KAFKA-17686
**Subsystem**: clients/src
**Category**: Missing null / bounds check

**Root cause**
The code to convert the Map was initially expressed using the Streams API with collect(Collectors.toMap()). Unfortunately, the implementation of Collector returned by Collectors.toMap() does not support null entries. The KafkaConsumer.offsetsForTimes() API explicitly states that nulls can be present in the returned Map, hence this change. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                return applicationEventHandler.addAndGet(listOffsetsEvent)
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().buildOffsetAndTimestamp()));
import kafka.utils.{Log4jController, TestUtils}
import org.junit.jupiter.params.provider.ValueSource
  @ParameterizedTest
  @ValueSource(strings = Array("kraft"))
  def testOffsetsForTimesAfterDeleteRecords(quorum: String): Unit = {
    val consumer = createConsumer()

// After
import java.util.HashMap;
                Map<TopicPartition, OffsetAndTimestampInternal> offsets = applicationEventHandler.addAndGet(listOffsetsEvent);
                Map<TopicPartition, OffsetAndTimestamp> results = new HashMap<>(offsets.size());
                offsets.forEach((k, v) -> results.put(k, v != null ? v.buildOffsetAndTimestamp() : null));
                return results;
import kafka.utils.{Log4jController, TestInfoUtils, TestUtils}
import org.junit.jupiter.params.provider.{Arguments, MethodSource, ValueSource}
  @ParameterizedTest(name = TestInfoUtils.TestWithParameterizedQuorumAndGroupProtocolNames)
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersAll"))
  def testOffsetsForTimesAfterDeleteRecords(quorum: String, groupProtocol: String): Unit = {
    val props = new Properties()
    val consumer = createConsumer(configOverrides = props)
```

**How to spot**
When reviewing clients/src code, look for missing null / bounds check patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17686 asynckafkaconsumer.offsetsfortimes() fails with nullpointerexception (#17353).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
