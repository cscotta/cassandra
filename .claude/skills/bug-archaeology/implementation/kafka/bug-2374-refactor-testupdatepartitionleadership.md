**Commit**: c95865437b · **Ticket**: KAFKA-16027
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
    synchronized Optional<MetadataResponse.PartitionMetadata> partitionMetadataIfCurrent(TopicPartition topicPartition) {
import org.apache.kafka.common.ClusterResourceListener;
import org.apache.kafka.common.requests.MetadataResponse.PartitionMetadata;
import org.apache.kafka.common.requests.RequestTestUtils.PartitionMetadataSupplier;
import org.mockito.Mockito;
import java.util.ArrayList;
import java.util.stream.Collectors;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
    @Test

// After
    public synchronized Optional<MetadataResponse.PartitionMetadata> partitionMetadataIfCurrent(TopicPartition topicPartition) {
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16027: refactor testupdatepartitionleadership (#17083).

**Test**
Test modified in MetadataTest.java covering the corrected behaviour.
