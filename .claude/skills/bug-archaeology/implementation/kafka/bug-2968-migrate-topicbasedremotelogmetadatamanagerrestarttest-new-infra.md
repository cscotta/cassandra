**Commit**: b05f82d444 · **Ticket**: KAFKA-16785
**Subsystem**: storage/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    public void initializeRemoteLogMetadataManager(Set<TopicIdPartition> topicIdPartitions,
                                                   boolean startConsumerThread,
                                                   Function<Integer, RemoteLogMetadataTopicPartitioner> remoteLogMetadataTopicPartitioner) {
        initializeRemoteLogMetadataManager(topicIdPartitions, startConsumerThread, remoteLogMetadataTopicPartitioner, RemotePartitionMetadataStore::new);
    }

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import java.io.IOException;

// After
          .overrideRemoteLogMetadataManagerProps(overrideRemoteLogMetadataManagerProps())
import kafka.test.ClusterInstance;
import kafka.test.annotation.ClusterTest;
import kafka.test.junit.ClusterTestExtensions;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
@ExtendWith(value = ClusterTestExtensions.class)
@Tag("integration")
    private final ClusterInstance clusterInstance;
    TopicBasedRemoteLogMetadataManagerRestartTest(ClusterInstance clusterInstance) {     // Constructor injections
```

**How to spot**
When reviewing storage/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16785 migrate topicbasedremotelogmetadatamanagerrestarttest to new test infra (#16170).

**Test**
Test modified in TopicBasedRemoteLogMetadataManagerHarness.java covering the corrected behaviour.
