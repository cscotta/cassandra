**Commit**: 0d9fe518b6 · **Ticket**: KAFKA-20434
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
When all static members of a consumer group using the consumer protocol
leave and rejoin with a different server-side assignor, the group does
not recompute the target assignment. The `bumpGroupEpoch` flag only
considers changes to subscribed topic names and regex but not the
preferred server assignor. This patch adds a `hasPreferredServerAssignorChanged` check that
compares the effective preferred assignor (falling back to the default
when no member has an explicit preference) before and after the member
update. The group epoch is bumped when the effective preferred assignor
changes, which only happens once a majority of members has switched.

**Before/After**
```java
// Before
import org.junit.jupiter.api.Assertions.{assertEquals, assertFalse, assertNotEquals, assertNotNull}

// After
import org.junit.jupiter.api.Assertions.{assertEquals, assertFalse, assertNotEquals, assertNotNull, assertTrue}

  @ClusterTest(
    serverProperties = Array(
      new ClusterConfigProperty(key = GroupCoordinatorConfig.CONSUMER_GROUP_ASSIGNMENT_INTERVAL_MS_CONFIG, value = "0"),
      new ClusterConfigProperty(key = GroupCoordinatorConfig.CONSUMER_GROUP_MIN_ASSIGNMENT_INTERVAL_MS_CONFIG, value = "0")
    )
  )
  def testStaticMembersRejoinWithDifferentServerAssignor(): Unit = {
    // Creates the __consumer_offsets topics because it won't be created automatically
    // in this test because it does not use FindCoordinator API.
    createOffsetsTopic()
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20434: consumer group does not recompute assignment when static members rejoin with different assignor (#22036).

**Test**
Test modified in ConsumerGroupHeartbeatRequestTest.scala covering the corrected behaviour.
