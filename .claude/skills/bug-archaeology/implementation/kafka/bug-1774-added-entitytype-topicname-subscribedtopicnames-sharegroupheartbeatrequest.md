**Commit**: 6e61bfee91 · **Ticket**: KAFKA-18312
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
This PR Adds entityType: topicName to SubscribedTopicNames in ShareGroupHeartbeatRequest.json as per the recent update to kip-932. Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
    { "name": "SubscribedTopicNames", "type": "[]string", "versions": "0+", "nullableVersions": "0+", "default": "null",

// After
    { "name": "SubscribedTopicNames", "type": "[]string", "versions": "0+", "nullableVersions": "0+", "default": "null", "entityType": "topicName",
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18312: added entitytype: topicname to subscribedtopicnames in sharegroupheartbeatrequest.json (#18285).

**Test**
No test added.
