**Commit**: 67f966f348 · **Ticket**: KAFKA-17469
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
            <allow pkg="org.apache.kafka.server.group.share" />
import org.apache.kafka.server.group.share.GroupTopicPartitionData;
import org.apache.kafka.server.group.share.PartitionAllData;
import org.apache.kafka.server.group.share.PartitionErrorData;
import org.apache.kafka.server.group.share.PartitionFactory;
import org.apache.kafka.server.group.share.PartitionIdLeaderEpochData;
import org.apache.kafka.server.group.share.PartitionStateBatchData;
import org.apache.kafka.server.group.share.Persister;
import org.apache.kafka.server.group.share.PersisterStateBatch;
import org.apache.kafka.server.group.share.ReadShareGroupStateParameters;
import org.apache.kafka.server.group.share.TopicData;
import org.apache.kafka.server.group.share.WriteShareGroupStateParameters;

// After
    implementation project(':share')
            <allow pkg="org.apache.kafka.server.share" />
  <allow pkg="org.apache.kafka.common.annotation" />
import org.apache.kafka.server.share.GroupTopicPartitionData;
import org.apache.kafka.server.share.PartitionAllData;
import org.apache.kafka.server.share.PartitionErrorData;
import org.apache.kafka.server.share.PartitionFactory;
import org.apache.kafka.server.share.PartitionIdLeaderEpochData;
import org.apache.kafka.server.share.PartitionStateBatchData;
import org.apache.kafka.server.share.Persister;
import org.apache.kafka.server.share.PersisterStateBatch;
import org.apache.kafka.server.share.ReadShareGroupStateParameters;
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17469: moved share external interfaces to share module. (#17262).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
