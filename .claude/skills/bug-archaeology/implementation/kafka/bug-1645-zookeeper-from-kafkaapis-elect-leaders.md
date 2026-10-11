**Commit**: ddc3faa88f · **Ticket**: KAFKA-18399
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.message.ElectLeadersResponseData.{PartitionResult, ReplicaElectionResult}
        case ApiKeys.ALTER_PARTITION => handleAlterPartitionRequest(request)
  def handleElectLeaders(request: RequestChannel.Request): Unit = {
    val zkSupport = metadataSupport.requireZkOrThrow(KafkaApis.shouldAlwaysForward(request))
    val electionRequest = request.body[ElectLeadersRequest]

    def sendResponseCallback(
      error: ApiError
    )(
      results: Map[TopicPartition, ApiError]
    ): Unit = {
      requestHelper.sendResponseMaybeThrottle(request, requestThrottleMs => {

// After
// (no additions)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18399 remove zookeeper from kafkaapis (8/n): elect_leaders , alter_partition, update_features (#18453).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
