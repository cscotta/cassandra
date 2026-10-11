**Commit**: 69ff5d1e70 · **Ticket**: KAFKA-18817
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Christo Lolov <lolovc@amazon.com>

**Before/After**
```java
// Before
          if (!authorizer.isEmpty) {
        shareGroupHeartbeatRequest.data,

  def testShareGroupHeartbeatRequestAuthorizationFailed(): Unit = {

// After
          if (authorizer.isDefined) {
      if (shareGroupHeartbeatRequest.data.subscribedTopicNames != null &&
        !shareGroupHeartbeatRequest.data.subscribedTopicNames.isEmpty) {
        // Check the authorization if the subscribed topic names are provided.
        // Clients are not allowed to see topics that are not authorized for Describe.
        val subscribedTopicSet = shareGroupHeartbeatRequest.data.subscribedTopicNames.asScala.toSet
        val authorizedTopics = authHelper.filterByAuthorized(request.context, DESCRIBE, TOPIC,
          subscribedTopicSet)(identity)
        if (authorizedTopics.size < subscribedTopicSet.size) {
          val responseData = new ShareGroupHeartbeatResponseData()
            .setErrorCode(Errors.TOPIC_AUTHORIZATION_FAILED.code)
          requestHelper.sendMaybeThrottle(request, new ShareGroupHeartbeatResponse(responseData))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18817: sharegroupheartbeat and sharegroupdescribe api must check topic describe (#19083).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
