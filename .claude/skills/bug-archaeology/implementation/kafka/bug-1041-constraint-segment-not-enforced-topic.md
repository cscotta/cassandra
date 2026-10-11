**Commit**: bcda92b5b9 · **Ticket**: KAFKA-19080
**Subsystem**: checkstyle/import-control.xml
**Category**: Logic error in condition

**Root cause**
The main issue was that we forgot to set
`TopicConfig.SEGMENT_BYTES_CONFIG` to at least `1024 * 1024`, which
caused problems in tests with small segment sizes. To address this, we introduced a new internal config:
`LogConfig.INTERNAL_SEGMENT_BYTES_CONFIG`, allowing us to set smaller
segment bytes specifically for testing purposes. We also updated the logic so that if a user configures the topic-level
segment bytes without explicitly setting the internal config, the
internal value will no longer be returned to the user. In addition, we removed
`MetadataLogConfig#METADATA_LOG_SEGMENT_MIN_BYTES_CONFIG` and added
three new internal configurations:
- `INTERNAL_MAX_BATCH_SIZE_IN_BYTES_CONFIG`
- `I...

**Before/After**
```java
// Before
      "For entity-type '" + TopicType + "': " + LogConfig.configNames.asScala.map("\t" + _).mkString(nl, nl, nl) +
import org.apache.kafka.raft.{Isolation, KafkaRaftClient, LogAppendInfo, LogFetchInfo, LogOffsetMetadata, MetadataLogConfig, OffsetMetadata, ReplicatedLog, SegmentPosition, ValidOffsetAndEpoch}
    val fetchInfo = log.read(startOffset, config.maxFetchSizeInBytes, isolation, true)
        config.deleteDelayMillis
    props.setProperty(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, config.maxBatchSizeInBytes.toString)
    props.setProperty(TopicConfig.SEGMENT_BYTES_CONFIG, config.logSegmentBytes.toString)
    props.setProperty(TopicConfig.SEGMENT_MS_CONFIG, config.logSegmentMillis.toString)
    if (config.logSegmentBytes < config.logSegmentMinBytes) {
      throw new InvalidConfigurationException(


// After
      <allow pkg="org.apache.kafka.storage"/>
      "For entity-type '" + TopicType + "': " + LogConfig.nonInternalConfigNames.asScala.map("\t" + _).mkString(nl, nl, nl) +
import org.apache.kafka.raft.{Isolation, LogAppendInfo, LogFetchInfo, LogOffsetMetadata, MetadataLogConfig, OffsetMetadata, ReplicatedLog, SegmentPosition, ValidOffsetAndEpoch}
    val fetchInfo = log.read(startOffset, config.internalMaxFetchSizeInBytes, isolation, true)
        config.internalDeleteDelayMillis
    props.setProperty(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, config.internalMaxBatchSizeInBytes.toString)
    if (config.internalSegmentBytes() != null)
      props.setProperty(LogConfig.INTERNAL_SEGMENT_BYTES_CONFIG, config.internalSegmentBytes().toString)
    else
      props.setProperty(TopicConfig.SEGMENT_BYTES_CONFIG, config.logSegmentBytes.toString)
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19080 the constraint on segment.ms is not enforced at topic level (#19371).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
