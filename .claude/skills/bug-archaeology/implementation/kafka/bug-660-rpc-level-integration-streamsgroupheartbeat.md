**Commit**: 53e117253d · **Ticket**: KAFKA-19807
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
## What
Ticket: https://issues.apache.org/jira/browse/KAFKA-19807
Follow up on https://github.com/apache/kafka/pull/20757, add tests that:
1. Test member with topology that triggers internal topic auto creation
2. Test member heartbeat under dynamic configuration
(`streams.num.standby.replicas`) change
3. Test membership expiring and rejoining
4.

**Before/After**
```java
// Before
import org.junit.jupiter.api.Assertions.{assertEquals, assertNotNull, assertThrows, assertTrue}
    new ClusterConfigProperty(key = "unstable.api.versions.enable", value = "true")

// After
import org.apache.kafka.clients.admin.{AlterConfigOp, ConfigEntry}
import org.apache.kafka.common.config.ConfigResource
import org.junit.jupiter.api.Assertions.{assertEquals, assertNotNull, assertNull, assertThrows, assertTrue}
    new ClusterConfigProperty(key = "unstable.api.versions.enable", value = "true"),
    new ClusterConfigProperty(key = "group.coordinator.rebalance.protocols", value = "classic,consumer,streams")
  @ClusterTest
  def testInternalTopicsCreation(): Unit = {
    val admin = cluster.admin()
    val memberId = "test-member-1"
    val groupId = "test-group"
    val inputTopicName = "input-topic"

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19807: add rpc-level integration tests for streamsgroupheartbeat [2/2] (#20762).

**Test**
Test modified in StreamsGroupHeartbeatRequestTest.scala covering the corrected behaviour.
