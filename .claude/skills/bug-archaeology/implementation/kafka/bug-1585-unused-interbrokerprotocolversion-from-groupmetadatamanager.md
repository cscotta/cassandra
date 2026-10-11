**Commit**: 485d36d3b3 · **Ticket**: KAFKA-18589
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    val groupMetadataManager = new GroupMetadataManager(config.brokerId, config.interBrokerProtocolVersion,
      offsetConfig, replicaManager, time, metrics)
import org.apache.kafka.server.common.{MetadataVersion, RequestLocal}
                           interBrokerProtocolVersion: MetadataVersion,
import org.apache.kafka.server.common.{MetadataVersion, RequestLocal}
    groupMetadataManager = new GroupMetadataManager(0, MetadataVersion.latestTesting, offsetConfig, replicaManager,
      time, metrics)
    val gmm = new GroupMetadataManager(0, MetadataVersion.latestTesting, offsetConfig, replicaManager, time, metrics) {

// After
    val groupMetadataManager = new GroupMetadataManager(config.brokerId, offsetConfig, replicaManager, time, metrics)
import org.apache.kafka.server.common.RequestLocal
import org.apache.kafka.server.common.RequestLocal
    groupMetadataManager = new GroupMetadataManager(0, offsetConfig, replicaManager, time, metrics)
    val gmm = new GroupMetadataManager(0, offsetConfig, replicaManager, time, metrics) {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular:  kafka-18589 remove unused interbrokerprotocolversion from groupmetadatamanager (#18619).

**Test**
Test modified in GroupMetadataManagerTest.scala covering the corrected behaviour.
