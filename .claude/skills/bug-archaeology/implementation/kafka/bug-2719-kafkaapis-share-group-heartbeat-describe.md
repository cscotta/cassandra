**Commit**: 3a442ffe32 · **Ticket**: KAFKA-16743
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Added handling of share group heartbeat and describe in KafkaApis. The Implementation of heartbeat and describe is with group coordinator. Reviewers:  Manikumar Reddy <manikumar.reddy@gmail.com>, Rahul <rahul.nirgude@mastercard.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.config.{ConfigType, KRaftConfigs, ReplicationConfigs, ServerConfigs, ServerLogConfigs}

// After
        case ApiKeys.SHARE_GROUP_HEARTBEAT => handleShareGroupHeartbeat(request).exceptionally(handleError)
        case ApiKeys.SHARE_GROUP_DESCRIBE => handleShareGroupDescribe(request).exceptionally(handleError)
  def handleShareGroupHeartbeat(request: RequestChannel.Request): CompletableFuture[Unit] = {
    val shareGroupHeartbeatRequest = request.body[ShareGroupHeartbeatRequest]

    if (!isShareGroupProtocolEnabled) {
      requestHelper.sendMaybeThrottle(request, shareGroupHeartbeatRequest.getErrorResponse(Errors.UNSUPPORTED_VERSION.exception))
      CompletableFuture.completedFuture[Unit](())
    } else if (!authHelper.authorize(request.context, READ, GROUP, shareGroupHeartbeatRequest.data.groupId)) {
      requestHelper.sendMaybeThrottle(request, shareGroupHeartbeatRequest.getErrorResponse(Errors.GROUP_AUTHORIZATION_FAILED.exception))
      CompletableFuture.completedFuture[Unit](())
    } else {
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16743,kafka-16744: kafkaapis support for share group heartbeat and describe (#16574).

**Test**
Test modified in KafkaApisTest.scala covering the corrected behaviour.
