**Commit**: 70eab7778d · **Ticket**: KAFKA-17894
**Subsystem**: checkstyle/import-control-server.xml
**Category**: Logic error in condition

**Root cause**
The PR implements the BrokerTopicMetrics defined in KIP-1103. The PR also corrected the share-acknowledgement-rate and share-acknowledgement-count metrics defined in KIP-932 as they are moved to BrokerTopicMetrics, necessary changes to KIP-932 broker metrics will be done once we complete KIP-1103. Reviewers: Andrew Schofield <aschofield@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
              files="(RemoteLogManagerTest|SharePartitionTest).java"/>
        Metrics metrics
            metrics
        Metrics metrics
            metrics
            Metrics metrics
        processShareFetch(new ShareFetch(fetchParams, groupId, memberId, future, partitionMaxBytes, batchSize, maxFetchRecords));

// After
    <subpackage name="share">
      <allow pkg="org.apache.kafka.storage.log.metrics" />
    </subpackage>
              files="(RemoteLogManagerTest|SharePartitionManagerTest|SharePartitionTest).java"/>
import org.apache.kafka.storage.log.metrics.BrokerTopicStats;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
    /**
     * The broker topic stats is used to record the broker topic metrics for share group.
     */
```

**How to spot**
When reviewing checkstyle/import-control-server.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17894: implemented broker topic metrics for share group 1/n (kip-1103) (#18444).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
