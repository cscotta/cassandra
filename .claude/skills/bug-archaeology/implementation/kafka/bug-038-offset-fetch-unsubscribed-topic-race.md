**Commit**: fef7f34f01 · **Ticket**: KAFKA-20040
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/internals/CommitRequestManager.java
**Category**: Race condition / missing synchronization

**Root cause**
When using topic IDs for `OffsetFetch` requests (introduced in 4.2), the response-parsing path resolved topic IDs back to names via `metadata.topicNames()`. If a consumer unsubscribed between sending the request and receiving the response, the topic was removed from consumer metadata, causing the lookup to return `null` and failing the future with "Topic does not exist" even though the failure was benign. The fix caches the topic-ID-to-name mapping at request-build time in `OffsetFetchRequestState.topicNamesCache` and uses that cache during response parsing instead of querying live metadata.

**Before/After**
```java
// Before – looks up topic name from live metadata (can be null after unsubscribe)
String topicName = topic.name().isEmpty()
    ? metadata.topicNames().get(topic.topicId()) : topic.name();
if (error != Errors.NONE || topicName == null) { ... fail ... }

// After – uses per-request cache; null topicName after unsubscribe is ignored
String topicName = Uuid.ZERO_UUID.equals(topic.topicId())
    ? topic.name() : topicNamesCache.get(topic.topicId());
if (error != Errors.NONE) { ... fail ... }
```

**How to spot**
Response-parsing code that resolves request context from shared mutable state (like `metadata.topicNames()`) is vulnerable to TOCTOU races when the consumer can unsubscribe concurrently. Request-scoped context must be captured at request-build time.

**Test**
`CommitRequestManagerTest#testFetchOffsetsWithTopicIdsDoesNotFailOnUnsubscribedTopics`: builds an OffsetFetch request for a topic, then returns a response with a topic ID that is no longer in the metadata mock; asserts the future completes successfully and `metadata.topicNames()` is never called.
