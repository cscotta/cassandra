**Commit**: 20e616ecc1 · **Ticket**: KAFKA-18578
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Ismael Juma <ismael@juma.me.uk>

**Before/After**
```java
// Before
import kafka.server.metadata.{KRaftMetadataCache}
import org.apache.kafka.common.message.UpdateMetadataRequestData.{UpdateMetadataBroker, UpdateMetadataEndpoint, UpdateMetadataPartitionState, UpdateMetadataTopicState}
import org.apache.kafka.common.protocol.{ApiKeys, ApiMessage, Errors}
import org.apache.kafka.common.requests.{AbstractControlRequest, UpdateMetadataRequest}
import org.apache.kafka.image.{ClusterImage, MetadataDelta, MetadataImage, MetadataProvenance}
import org.apache.kafka.metadata.{LeaderAndIsr, LeaderRecoveryState}
          ClusterImage.EMPTY,

  def updateCache(cache: MetadataCache, request: UpdateMetadataRequest, records: Seq[ApiMessage] = List()): Unit = {
    cache match {
      case c: KRaftMetadataCache => {
        // UpdateMetadataRequest always contains a full list of brokers, but may contain

// After
import kafka.server.metadata.KRaftMetadataCache
import org.apache.kafka.common.protocol.{ApiMessage, Errors}
import org.apache.kafka.image.{MetadataDelta, MetadataImage, MetadataProvenance}
import org.apache.kafka.metadata.LeaderRecoveryState
          image.cluster(),
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18578: remove `updatemetadatarequest` from `metadatacachetest` (#18628).

**Test**
Test modified in MetadataCacheTest.scala covering the corrected behaviour.
