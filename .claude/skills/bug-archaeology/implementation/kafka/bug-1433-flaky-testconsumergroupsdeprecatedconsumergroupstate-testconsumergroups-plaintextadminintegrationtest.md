**Commit**: 2bbd25841e · **Ticket**: KAFKA-18298
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
It's related to KAFKA-18298 and KAFKA-18297. The root cause of the flaky tests is member rejoin after member removal. To prevent members from rejoining after being removed, before removing group members, calling `consumers.close` in ConsumerThread . This fix also extract the flaky member removal test  to new test `testConsumerGroupWithMemberRemoval`.

**Before/After**
```java
// Before
import java.util.{Collections, Optional, Properties}
      val EMPTY_GROUP_INSTANCE_ID = ""
      def createProperties(groupInstanceId: String): Properties = {
        val newConsumerConfig = new Properties(consumerConfig)
        // We need to disable the auto commit because after the members got removed from group, the offset commit
        // will cause the member rejoining and the test will be flaky (check ConsumerCoordinator#OffsetCommitResponseHandler)
        newConsumerConfig.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
        newConsumerConfig.setProperty(ConsumerConfig.GROUP_ID_CONFIG, testGroupId)
        newConsumerConfig.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, testClientId)
        if (groupInstanceId != EMPTY_GROUP_INSTANCE_ID) {
          newConsumerConfig.setProperty(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, groupInstanceId)
        }

// After
import java.util.{Collections, Locale, Optional, Properties}
      val groupInstanceSet = Set(testInstanceId1, testInstanceId2, "")
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18298 fix flaky testconsumergroupsdeprecatedconsumergroupstate and testconsumergroups in plaintextadminintegration.

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
