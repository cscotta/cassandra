**Commit**: 36f19057e1 · **Ticket**: KAFKA-18813
**Subsystem**: checkstyle/import-control-coordinator-common.xml
**Category**: Logic error in condition

**Root cause**
This patch filters out the topic describe unauthorized topics from the
ConsumerGroupHeartbeat and ConsumerGroupDescribe response. In ConsumerGroupHeartbeat, 
- if the request has `subscribedTopicNames` set, we directly check the
authz in `KafkaApis` and return a topic auth failure in the response if
any of the topics is denied. - Otherwise, we check the authz only if a regex refresh is triggered and
we do it based on the acl of the consumer that triggered the refresh. If
any of the topic is denied, we filter it out from the resolved
subscription.

**Before/After**
```java
// Before
// (no removals)

// After
                <allow pkg="org.apache.kafka.server.authorizer" />
    <allow pkg="org.apache.kafka.common.acl" />
    <allow pkg="org.apache.kafka.common.resource" />
            <allow pkg="org.apache.kafka.server.authorizer"/>
            <allow pkg="org.apache.kafka.server.authorizer" />
  // - TOPIC_AUTHORIZATION_FAILED (version 0+)
  // - TOPIC_AUTHORIZATION_FAILED (version 0+)
        .withAuthorizer(authorizer.toJava)
```

**How to spot**
When reviewing checkstyle/import-control-coordinator-common.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18813: consumergroupheartbeat api and consumergroupdescribe api must check topic describe (#18989).

**Test**
Test modified in AuthorizerIntegrationTest.scala covering the corrected behaviour.
