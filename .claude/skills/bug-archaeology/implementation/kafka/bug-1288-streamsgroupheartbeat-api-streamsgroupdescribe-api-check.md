**Commit**: e73719d962 · **Ticket**: KAFKA-18819
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
This patch filters out the topic describe unauthorized topics from the
StreamsGroupHeartbeat and StreamsGroupDescribe response. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before
  // - TOPIC_AUTHORIZATION_FAILED (version 0+)
        streamsGroupHeartbeatRequest.data,

// After
            case TOPIC_AUTHORIZATION_FAILED:
 * - {@link Errors#TOPIC_AUTHORIZATION_FAILED}
  // - TOPIC_AUTHORIZATION_FAILED (version 0+)
  // - TOPIC_AUTHORIZATION_FAILED (version 0+)

        if (requiredTopics.nonEmpty) {
          val authorizedTopics = authHelper.filterByAuthorized(request.context, DESCRIBE, TOPIC, requiredTopics)(identity)
          if (authorizedTopics.size < requiredTopics.size) {
            val responseData = new StreamsGroupHeartbeatResponseData().setErrorCode(Errors.TOPIC_AUTHORIZATION_FAILED.code)
            requestHelper.sendMaybeThrottle(request, new StreamsGroupHeartbeatResponse(responseData))
            return CompletableFuture.completedFuture[Unit](())
          }
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18819 streamsgroupheartbeat api and streamsgroupdescribe api  check topic describe (#19183).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
