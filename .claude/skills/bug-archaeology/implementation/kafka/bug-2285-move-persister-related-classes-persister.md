**Commit**: d173842d36 · **Ticket**: KAFKA-17469
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.share.GroupTopicPartitionData;
import org.apache.kafka.server.share.PartitionAllData;
import org.apache.kafka.server.share.PartitionErrorData;
import org.apache.kafka.server.share.PartitionFactory;
import org.apache.kafka.server.share.PartitionIdLeaderEpochData;
import org.apache.kafka.server.share.PartitionStateBatchData;
import org.apache.kafka.server.share.Persister;
import org.apache.kafka.server.share.PersisterStateBatch;
import org.apache.kafka.server.share.ReadShareGroupStateParameters;
import org.apache.kafka.server.share.TopicData;
import org.apache.kafka.server.share.WriteShareGroupStateParameters;
import org.apache.kafka.server.share.Persister;

// After
import org.apache.kafka.server.share.persister.GroupTopicPartitionData;
import org.apache.kafka.server.share.persister.PartitionAllData;
import org.apache.kafka.server.share.persister.PartitionErrorData;
import org.apache.kafka.server.share.persister.PartitionFactory;
import org.apache.kafka.server.share.persister.PartitionIdLeaderEpochData;
import org.apache.kafka.server.share.persister.PartitionStateBatchData;
import org.apache.kafka.server.share.persister.Persister;
import org.apache.kafka.server.share.persister.PersisterStateBatch;
import org.apache.kafka.server.share.persister.ReadShareGroupStateParameters;
import org.apache.kafka.server.share.persister.TopicData;
import org.apache.kafka.server.share.persister.WriteShareGroupStateParameters;
import org.apache.kafka.server.share.persister.Persister;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17469: move persister related classes to persister pkg. (#17349).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
