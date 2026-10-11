**Commit**: 69a457d8a5 · **Ticket**: KAFKA-19034
**Subsystem**: checkstyle/import-control-storage.xml
**Category**: Logic error in condition

**Root cause**
This PR rewrites `RemoteTopicCrudTest` in Java using the `@ClusterTest`
framework and moves it to the `storage` module. **Note:** Two test cases have not yet been migrated

- `testClusterWideDisablementOfTieredStorageWithEnabledTieredTopic`
-

`testClusterWithoutTieredStorageStartsSuccessfullyIfTopicWithTieringDisabled`

These tests rely on modifying broker configs during the test lifecycle,
which `ClusterTest` currently does not support. They will be migrated in
a follow-up PR after
[#16808](https://github.com/apache/kafka/pull/16808) is merged, which
introduces support for config updates in `ClusterTest`. Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.clients.admin.{AlterConfigOp, ConfigEntry}
import org.apache.kafka.common.{TopicIdPartition, TopicPartition, Uuid}
import org.apache.kafka.common.config.{ConfigException, ConfigResource, TopicConfig}
import org.apache.kafka.common.errors.{InvalidConfigurationException, UnknownTopicOrPartitionException}
import org.apache.kafka.common.utils.MockTime
import org.apache.kafka.server.log.remote.storage.{NoOpRemoteLogMetadataManager, NoOpRemoteStorageManager, RemoteLogManagerConfig, RemoteLogSegmentId, RemoteLogSegmentMetadata, RemoteLogSegmentState}
import org.junit.jupiter.api.function.Executable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util
import java.util.concurrent.atomic.AtomicInteger
import java.util.{Optional, Properties}

// After
    <subpackage name="admin">
        <allow pkg="org.apache.kafka.clients.admin" class="Admin" />
        <allow pkg="org.apache.kafka.common.config" />
        <allow pkg="org.apache.kafka.server.log.remote.storage" />
        <allow pkg="scala.jdk.javaapi" />
        <allow pkg="org.apache.kafka.test" />

    </subpackage>

import org.apache.kafka.common.config.{ConfigException, TopicConfig}
import org.apache.kafka.server.log.remote.storage._
import java.util.Properties
```

**How to spot**
When reviewing checkstyle/import-control-storage.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19034 [1/n] rewrite remotetopiccrudtest by clustertest and move it to storage module (#19681).

**Test**
Test modified in RemoteTopicCrudTest.scala covering the corrected behaviour.
