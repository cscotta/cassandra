**Commit**: 32efc1cd09 · **Ticket**: KAFKA-19755
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Move KRaftClusterTest from core module to server module. Rewrite
- testCreateClusterAndPerformReassignment
- testIncrementalAlterConfigs
- testSetLog4jConfigurations
- testCreatePartitions
- testDescribeQuorumRequestToBrokers
- testDescribeQuorumRequestToControllers
- testUpdateMetadataVersion
- testDescribeKRaftVersion
- testCreateClusterAndCreateTopicWithRemoteLogManagerInstantiation

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.errors.{InvalidPartitionsException, PolicyViolationException, UnsupportedVersionException}
import org.apache.kafka.common.protocol.Errors._
import org.apache.kafka.common.requests.ApiError
import org.apache.kafka.common.{TopicPartition, TopicPartitionInfo}
import org.apache.kafka.server.common.{ApiMessageAndVersion, KRaftVersion, MetadataVersion}
import org.apache.kafka.server.log.remote.storage.RemoteLogManagerConfig
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory
import java.util.{Optional, OptionalLong}
  val log = LoggerFactory.getLogger(classOf[KRaftClusterTest])
  val log2 = LoggerFactory.getLogger(classOf[KRaftClusterTest].getCanonicalName + "2")

// After
import org.apache.kafka.common.errors.{PolicyViolationException, UnsupportedVersionException}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.server.common.{ApiMessageAndVersion, MetadataVersion}
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19755 move kraftclustertest from core module to server module [3/4] (#21175).

**Test**
Test modified in KRaftClusterTest.scala covering the corrected behaviour.
