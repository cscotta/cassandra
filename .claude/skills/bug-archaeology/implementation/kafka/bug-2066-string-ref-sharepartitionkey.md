**Commit**: 2e2b0a58ed · **Ticket**: KAFKA-17914
**Subsystem**: core/src
**Category**: Wrong serialization / deserialization

**Root cause**
Currently, we are using the String repr of the shareCoordinator/sharePartition key (groupId:topicId:parition) as defined in kip-932 in a few methods like ShareCoordinator.partitionFor and ShareCoordinatorMetadataCacheHelper.getShareCoordinator. This has the potential to introduce subtle bugs when incorrect strings are used to invoke these methods. What is perturbing is that the failures might be intermittent. This PR aims to remedy the situation by changing the type to the concrete SharePartitionKey.

**Before/After**
```java
// Before
import org.apache.kafka.server.share.ErroneousAndValidPartitionData
          // None check already done above
          (shareCoordinator.get.partitionFor(key), SHARE_GROUP_STATE_TOPIC_NAME)
    private final Function<String, Integer> keyToPartitionMapper;
        Function<String, Integer> keyToPartitionMapper,
    public Node getShareCoordinator(String key, String internalTopicName) {

// After
import org.apache.kafka.server.share.{ErroneousAndValidPartitionData, SharePartitionKey}
        try {
          SharePartitionKey.validate(key)
        } catch {
          case e: IllegalArgumentException =>
            error(s"Share coordinator key is invalid", e)
            (Errors.INVALID_REQUEST, Node.noNode())
        }
          (shareCoordinator.foreach(coordinator => coordinator.partitionFor(SharePartitionKey.getInstance(key))), SHARE_GROUP_STATE_TOPIC_NAME)
import org.apache.kafka.server.share.SharePartitionKey;
    private final Function<SharePartitionKey, Integer> keyToPartitionMapper;
        Function<SharePartitionKey, Integer> keyToPartitionMapper,
```

**How to spot**
When reviewing core/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17914: update string ref with sharepartitionkey. (#17660).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
