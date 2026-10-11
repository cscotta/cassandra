**Commit**: cbfbbe833d · **Ticket**: KAFKA-19234
**Subsystem**: core/src/main/scala/kafka/server/KafkaApis.scala
**Category**: Incorrect filtering / wrong query result

**Root cause**: The produce-request handler checked `metadataCache.contains(topicPartition)` *before* authorisation. A client lacking WRITE permission on a topic received `UNKNOWN_TOPIC_OR_PARTITION` if the topic existed (revealing its existence) or proceeded to authorisation check which returned `TOPIC_AUTHORIZATION_FAILED`. This inconsistency leaked topic existence information. The fix moves the `metadataCache.contains` check to *after* the authorisation check, so an unauthorised client always gets `TOPIC_AUTHORIZATION_FAILED` regardless of whether the topic exists, matching the behaviour of the FETCH handler.

**Before/After**

```scala
// Before – existence check before auth
else if (!metadataCache.contains(topicPartition))
    nonExistingTopicResponses += ... -> new PartitionResponse(Errors.UNKNOWN_TOPIC_OR_PARTITION)
else
    topicIdToPartitionData += ...
// auth check happens later

// After – auth check first, existence check after
if (!authorizedTopics.contains(topicIdPartition.topic))
    unauthorizedTopicResponses += ... -> TOPIC_AUTHORIZATION_FAILED
else if (!metadataCache.contains(topicIdPartition.topicPartition))
    nonExistingTopicResponses += ... -> UNKNOWN_TOPIC_OR_PARTITION
else
    ...
```

**How to spot**: Whenever a handler returns different errors for "topic doesn't exist" vs "not authorised" before checking authorisation, topic names can be enumerated by unauthenticated clients. Always authorise before revealing existence.

**Test**: `testAuthorizationProduceVersionFromOldestTo12` — sends produce requests both with and without the topic existing; asserts `TOPIC_AUTHORIZATION_FAILED` is always returned when the client lacks permission, never `UNKNOWN_TOPIC_OR_PARTITION`.
