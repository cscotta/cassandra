**Commit**: 50fb993ce0 · **Ticket**: KAFKA-19136
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Separates metadata-related configurations from the `KRaftConfigs` into
the `MetadataLogConfig` class. Previously, metadata-related configs were placed in `KRaftConfigs`,
which mixed server-related configs (like process.roles) with
metadata-specific ones (like metadata.log.*), leading to confusion and
tight coupling. In this PR:
- Extract metadata-related config definitions and variables from
`KRaftConfig` into `MetadataLogConfig`. - Move `node.id` out of `MetadataLogConfig` into `KafkaMetadataLog’s
constructor` to avoid redundant config references.

**Before/After**
```java
// Before
import org.apache.kafka.server.config.{KRaftConfigs, ServerLogConfigs}
  config: MetadataLogConfig
  this.logIdent = s"[MetadataLog partition=$topicPartition, nodeId=${config.nodeId}] "
    config: MetadataLogConfig
        s"Cannot set ${KRaftConfigs.METADATA_LOG_SEGMENT_BYTES_CONFIG} below ${config.logSegmentMinBytes}: ${config.logSegmentBytes}"
      config
      metadataLog.error(s"Overriding ${KRaftConfigs.METADATA_LOG_SEGMENT_MIN_BYTES_CONFIG} is only supported for testing. Setting " +

// After
    implementation project(':raft')
    <allow pkg="org.apache.kafka.raft" />
import org.apache.kafka.server.config.ServerLogConfigs
  config: MetadataLogConfig,
  nodeId: Int
  this.logIdent = s"[MetadataLog partition=$topicPartition, nodeId=$nodeId] "
    config: MetadataLogConfig,
    nodeId: Int
        s"Cannot set ${MetadataLogConfig.METADATA_LOG_SEGMENT_BYTES_CONFIG} below ${config.logSegmentMinBytes}: ${config.logSegmentBytes}"
      config,
      nodeId
      metadataLog.error(s"Overriding ${MetadataLogConfig.METADATA_LOG_SEGMENT_MIN_BYTES_CONFIG} is only supported for testing. Setting " +
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19136 move metadata-related configs from kraftconfigs to metadatalogconfig (#19465).

**Test**
Test modified in IntegrationTestHarness.scala covering the corrected behaviour.
