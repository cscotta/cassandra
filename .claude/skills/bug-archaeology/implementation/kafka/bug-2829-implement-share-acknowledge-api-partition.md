**Commit**: f2a552a1eb · **Ticket**: KAFKA-16753
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The share-partition leader keeps track of the state and delivery attempts for in-flight records. However, delivery attempts tracking follows atleast-once semantics. The consumer processes the records and acknowledges them upon successful consumption. This successful attempt triggers a transition to the "Acknowledged" state.

**Before/After**
```java
// Before
        CompletableFuture<Optional<Throwable>> future = new CompletableFuture<>();
        future.completeExceptionally(new UnsupportedOperationException("Not implemented"));
        return future;

// After
import org.apache.kafka.common.errors.InvalidRecordStateException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.server.group.share.GroupTopicPartitionData;
import org.apache.kafka.server.group.share.PartitionErrorData;
import org.apache.kafka.server.group.share.PartitionFactory;
import org.apache.kafka.server.group.share.PartitionStateBatchData;
import org.apache.kafka.server.group.share.Persister;
import org.apache.kafka.server.group.share.PersisterStateBatch;
import org.apache.kafka.server.group.share.TopicData;
import org.apache.kafka.server.group.share.WriteShareGroupStateParameters;
import org.apache.kafka.server.group.share.WriteShareGroupStateResult;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16753: implement share acknowledge api in partition (kip-932) (#16339).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
