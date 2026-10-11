**Commit**: a3aa6372ea · **Ticket**: KAFKA-17336
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
- due to the server config UNSTABLE_API_VERSIONS_ENABLE_CONFIG is true, so we can't test the scenario of ListOffsetsRequest is unstable version. We want to test this case in this PR
- get the MV from metadataCache.metadataVersion() instead of config.interBrokerProtocolVersion since MV can be set dynamically. Reviewers: Jun Rao <junrao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      interBrokerProtocolVersion = replicaManager.config.interBrokerProtocolVersion,
import org.apache.kafka.server.common.MetadataVersion.IBP_2_8_IV0
  private val writeTxnMarkersRequestVersion: Short =
    if (config.interBrokerProtocolVersion.isAtLeast(IBP_2_8_IV0)) 1
    else 0

      val request = new WriteTxnMarkersRequest.Builder(writeTxnMarkersRequestVersion, markersToSend)
    } else if (offsetCommitRequest.data.groupInstanceId != null && config.interBrokerProtocolVersion.isLessThan(IBP_2_3_IV0)) {
    if (joinGroupRequest.data.groupInstanceId != null && config.interBrokerProtocolVersion.isLessThan(IBP_2_3_IV0)) {
    if (syncGroupRequest.data.groupInstanceId != null && config.interBrokerProtocolVersion.isLessThan(IBP_2_3_IV0)) {

// After
      interBrokerProtocolVersion = replicaManager.metadataCache.metadataVersion(),
      val request = new WriteTxnMarkersRequest.Builder(
        metadataCache.metadataVersion().writeTxnMarkersRequestVersion(), markersToSend
      )
    } else if (offsetCommitRequest.data.groupInstanceId != null && metadataCache.metadataVersion().isLessThan(IBP_2_3_IV0)) {
    if (joinGroupRequest.data.groupInstanceId != null && metadataCache.metadataVersion().isLessThan(IBP_2_3_IV0)) {
    if (syncGroupRequest.data.groupInstanceId != null && metadataCache.metadataVersion().isLessThan(IBP_2_3_IV0)) {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17336 add it to make sure the production mv does not use unstable version of list_offset  (#16893).

**Test**
Test modified in TransactionCoordinatorConcurrencyTest.scala covering the corrected behaviour.
