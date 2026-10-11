**Commit**: 00cd9cd3f9 · **Ticket**: KAFKA-18003
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Divij Vaidya <diviv@amazon.com>

**Before/After**
```java
// Before
import org.apache.kafka.storage.internals.log.{CleanerConfig, LogConfig}

// After
import java.nio.ByteBuffer
import java.nio.file.{Files, Paths, StandardOpenOption}
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.record.FileRecords
import org.apache.kafka.storage.internals.log.{CleanerConfig, LogConfig, LogFileUtils}
  @ParameterizedTest(name = TestInfoUtils.TestWithParameterizedQuorumAndGroupProtocolNames)
  @MethodSource(Array("getTestQuorumAndGroupProtocolParametersClassicGroupProtocolOnly"))
  def testDeleteRecordsAfterCorruptRecords(quorum: String, groupProtocol: String): Unit = {
    val config = new Properties()
    config.put(TopicConfig.SEGMENT_BYTES_CONFIG, "200")
    createTopic(topic, numPartitions = 1, replicationFactor = 1, config)

```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18003: add test to make sure `admin#deleterecords` can handle the corrupted records (#17840).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
