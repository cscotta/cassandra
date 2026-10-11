**Commit**: be7415cb8b · **Ticket**: KAFKA-18555
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import kafka.server.metadata.KRaftMetadataCache;
    KRaftMetadataCache metadataCache;
        KRaftMetadataCache metadataCache,
        DescribeTopicPartitionsResponseData response = metadataCache.getTopicMetadataForDescribeTopicResponse(
import kafka.server.metadata.KRaftMetadataCache
    metadataCache match {
      // In KRaft mode, only a replica which meets all of the following requirements is allowed to join the ISR.
      // 1. It is not fenced.
      // 2. It is not in controlled shutdown.
      // 3. Its metadata cached broker epoch matches its Fetch request broker epoch. Or the Fetch
      //    request broker epoch is -1 which bypasses the epoch verification.
      case kRaftMetadataCache: KRaftMetadataCache =>

// After
import kafka.server.MetadataCache;
    MetadataCache metadataCache;
        MetadataCache metadataCache,
        DescribeTopicPartitionsResponseData response = metadataCache.describeTopicResponse(
    // A replica which meets all of the following requirements is allowed to join the ISR.
    // 1. It is not fenced.
    // 2. It is not in controlled shutdown.
    // 3. Its metadata cached broker epoch matches its Fetch request broker epoch. Or the Fetch
    //    request broker epoch is -1 which bypasses the epoch verification.
    val mayBeReplica = getReplica(followerReplicaId)
    // The topic is already deleted and we don't have any replica information. In this case, we can return false
    // so as to avoid NPE
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18555 avoid casting metadatacache to kraftmetadatacache (#18632).

**Test**
Test modified in PartitionLockTest.scala covering the corrected behaviour.
