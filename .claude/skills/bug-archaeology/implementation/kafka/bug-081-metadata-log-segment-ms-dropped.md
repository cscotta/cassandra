**Commit**: 08aa469af7 · **Ticket**: KAFKA-19392
**Subsystem**: core/src/main/scala/kafka/raft/KafkaMetadataLog.scala
**Category**: State not cleaned up

**Root cause**: A prior refactor (#19371) accidentally deleted the line `props.setProperty(TopicConfig.SEGMENT_MS_CONFIG, config.logSegmentMillis.toString)` from the `KafkaMetadataLog` constructor's properties-building block. As a result, `metadata.log.segment.ms` was never passed to the underlying `UnifiedLog`, so the metadata log always used the default segment roll time regardless of user configuration. The one-liner was re-added in the same position it originally occupied.

**Before/After**

```scala
// Before – SEGMENT_MS_CONFIG line missing
props.setProperty(TopicConfig.SEGMENT_BYTES_CONFIG, config.logSegmentBytes.toString)
props.setProperty(TopicConfig.FILE_DELETE_DELAY_MS_CONFIG, ...)

// After – line restored
props.setProperty(TopicConfig.SEGMENT_BYTES_CONFIG, config.logSegmentBytes.toString)
props.setProperty(TopicConfig.SEGMENT_MS_CONFIG, config.logSegmentMillis.toString)
props.setProperty(TopicConfig.FILE_DELETE_DELAY_MS_CONFIG, ...)
```

**How to spot**: Property-building blocks where each config key is set individually are fragile; a missed line causes silent use of the default value. Comparing the set of keys written against the full config schema catches omissions.

**Test**: Builds a `KafkaMetadataLog` with default configuration and asserts that `log.config().segmentMs` equals `DefaultMetadataLogConfig.logSegmentMillis`, confirming the property is actually wired through.
