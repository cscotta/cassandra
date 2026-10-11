**Commit**: af012e1ec2 · **Ticket**: KAFKA-18961
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Consumers can subscribe to an RE2J SubscriptionPattern that will be
resolved and maintained on the server-side (KIP-848). Currently, those
regexes are refreshed on the coordinator when a consumer subscribes to a
new regex, or if there is a new topic metadata image (to ensure regex
resolution stays up-to-date with existing topics)

But with
[KAFKA-18813](https://issues.apache.org/jira/browse/KAFKA-18813), the
topics matching a regex are filtered based on ACLs. This generates a new
situation, as regexes resolution do not stay up-to-date as topics become
visible (ACLs added/delete). This patch introduces time-based refresh for the subscribed regex by
- Adding internal `group.consumer.regex.batc...

**Before/After**
```java
// Before
    member1Response = sendAndReceiveRegexHeartbeat(member1Response, interBrokerListenerName, None, fullRequest = true)

// After
    properties.put(GroupCoordinatorConfig.CONSUMER_GROUP_REGEX_REFRESH_INTERVAL_MS_CONFIG, "10000")
  @Test
  def testConsumerGroupHeartbeatWithRegexWithTopicDescribeAclAddedAndRemoved(): Unit = {
    createTopicWithBrokerPrincipal(topic)
    val allowAllOpsAcl = new AccessControlEntry(clientPrincipalString, WILDCARD_HOST, ALL, ALLOW)
    addAndVerifyAcls(Set(allowAllOpsAcl), groupResource)

    val memberId = Uuid.randomUuid.toString;
    var response = sendAndReceiveFirstRegexHeartbeat(memberId, listenerName)
    TestUtils.tryUntilNoAssertionError() {
      response = sendAndReceiveRegexHeartbeat(response, listenerName, Some(0), true)
    }
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18961: time-based refresh for server-side re2j regex (#19904).

**Test**
Test modified in AbstractAuthorizerIntegrationTest.scala covering the corrected behaviour.
