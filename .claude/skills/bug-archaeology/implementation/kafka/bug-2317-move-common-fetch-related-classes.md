**Commit**: 05366d2fa7 · **Ticket**: KAFKA-17626
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
    implementation project(':storage')
        <allow class="org.apache.kafka.storage.internals.log.FetchParams"/>
import org.apache.kafka.storage.internals.log.FetchIsolation;
import org.apache.kafka.storage.internals.log.FetchPartitionData;
    private Map<TopicIdPartition, FetchRequest.PartitionData> topicPartitionDataFromTryComplete = new LinkedHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(DelayedShareFetch.class);
import org.apache.kafka.storage.internals.log.FetchPartitionData;
import org.apache.kafka.storage.internals.log.FetchPartitionData;

// After
        <allow class="org.apache.kafka.server.storage.log.FetchParams"/>
import org.apache.kafka.server.storage.log.FetchIsolation;
import org.apache.kafka.server.storage.log.FetchPartitionData;
    private static final Logger log = LoggerFactory.getLogger(DelayedShareFetch.class);

    private Map<TopicIdPartition, FetchRequest.PartitionData> topicPartitionDataFromTryComplete;
        this.topicPartitionDataFromTryComplete = new LinkedHashMap<>();
import org.apache.kafka.server.storage.log.FetchPartitionData;
import org.apache.kafka.server.storage.log.FetchPartitionData;
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17626: move common fetch related classes from storage to server-common (#17289).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
