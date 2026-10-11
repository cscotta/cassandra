**Commit**: ef4b04b548 · **Ticket**: KAFKA-19844
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
* When a ShareFetch request contains RENEW acks, the fetchMessages
sub-routine is skipped in the KafkaApis handler for the request. * Additionally, new validations for ShareFetch version and ackType have
been added along with validations on fields maxBytes, minBytes,
maxRecords and maxWaitMs which should be set to 0 for version >= 2 and
isRenewAck set to true. * Unit tests have been added to verify the behavior. Reviewers: Abhinav Dixit <adixit@confluent.io>, poorv Mittal
 <apoorvmittal10@gmail.com>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
    val fetchResult: CompletableFuture[Map[TopicIdPartition, ShareFetchResponseData.PartitionData]] = handleFetchFromShareFetchRequest(
      request,
      shareSessionEpoch,
      erroneousAndValidPartitionData,
      sharePartitionManager,
      authorizedTopics
    )
                             memberId: String): CompletableFuture[Map[TopicIdPartition, ShareAcknowledgeResponseData.PartitionData]] = {
    val erroneousTopicIdPartitions = validateAcknowledgementBatches(acknowledgementData, erroneous)
    handleAcknowledgements(acknowledgementDataFromRequest, erroneous, sharePartitionManager, authorizedTopi

// After
    // KIP-1222 enforces setting the maxBytes, minBytes, maxRecords, maxWaitMs
    // values to 0, in case isRenewAck is true.
    if (shareFetchRequest.version >= 2 && shareFetchRequest.data.isRenewAck) {
      val reqData = shareFetchRequest.data
      var errorMsg: String = ""
      if (reqData.maxBytes != 0) {
        errorMsg += "maxBytes must be set to 0, "
      }

      if (reqData.minBytes != 0) {
        errorMsg += "minBytes must be set to 0, "
      }
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19844: sharefetch related changes to support renew. (#20826).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
