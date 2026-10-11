**Commit**: 101e15bb1c · **Ticket**: KAFKA-18867
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    client.describeConfigs(Collections.singletonList(existingTopic)).values.get(existingTopic).get()
    val nonExistentTopic = new ConfigResource(ConfigResource.Type.TOPIC, "unknown")
    val describeResult1 = client.describeConfigs(Collections.singletonList(nonExistentTopic))
    assertTrue(assertThrows(classOf[ExecutionException], () => describeResult1.values.get(nonExistentTopic).get).getCause.isInstanceOf[UnknownTopicOrPartitionException])
    val describeResult2 = client.describeConfigs(Collections.singletonList(invalidTopic))

    assertTrue(assertThrows(classOf[ExecutionException], () => describeResult2.values.get(invalidTopic).get).getCause.isInstanceOf[InvalidTopicException])

// After
    client.describeConfigs(util.List.of(existingTopic)).values.get(existingTopic).get()
    val defaultTopic = new ConfigResource(ConfigResource.Type.TOPIC, "")
    var describeResult = client.describeConfigs(util.List.of(defaultTopic))
    assertFutureThrows(classOf[InvalidTopicException], describeResult.all())
    val nonExistentTopic = new ConfigResource(ConfigResource.Type.TOPIC, "unknown")
    describeResult = client.describeConfigs(util.List.of(nonExistentTopic))
    assertFutureThrows(classOf[UnknownTopicOrPartitionException], describeResult.all())
    describeResult = client.describeConfigs(util.List.of(invalidTopic))
    assertFutureThrows(classOf[InvalidTopicException], describeResult.all())
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18867 add tests to describe topic configs with empty name (#19075).

**Test**
Test modified in PlaintextAdminIntegrationTest.scala covering the corrected behaviour.
