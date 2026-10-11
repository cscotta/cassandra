**Commit**: ada5f22bdf · **Ticket**: KAFKA-20165
**Subsystem**: clients — CommitRequestManager / OffsetFetch
**Category**: Incorrect filtering / wrong query result

**Root cause**
With OffsetFetch v10+, the Group Coordinator uses topic IDs instead of topic names. When a topic is deleted, the GC returns `UNKNOWN_TOPIC_ID` as a partition-level error. Before this fix, the consumer treated `UNKNOWN_TOPIC_ID` as a fatal error and threw a `KafkaException` from `poll()` or `committed()`. With topic names (v9 and below) the GC simply omits the partition or returns offset -1 — so this case never arose with the classic consumer. The fix handles `UNKNOWN_TOPIC_ID` as a retriable partition-level error: the consumer retries while time allows, and if the error persists returns `null` for those partitions (matching the `committed()` contract) rather than failing fatally.

**Before/After**
```java
// Before — UNKNOWN_TOPIC_ID treated as fatal, thrown from poll/committed
// (implicit: no special handling in CommitRequestManager partition error path)

// After — UNKNOWN_TOPIC_ID treated as retriable partition error
// commitRequestManager retries while deadline not exceeded;
// on timeout, partitions with persistent errors return null offset
```

**How to spot**
Any place that maps OffsetFetch partition-level errors to consumer exceptions should distinguish retriable vs. fatal errors. `UNKNOWN_TOPIC_ID` is retriable (topic may reappear, or the consumer may eventually give up gracefully). Compare the error-handling table between topic-name-based and topic-ID-based OffsetFetch response parsing.

**Test**
`PlaintextConsumerAssignTest#testAsyncPollAfterTopicDeleted` and `PlaintextConsumerCommitTest#testAsyncConsumerCommittedDeletedTopic` — integration tests that delete a topic after committing offsets, then assert that `poll()` does not throw and `committed()` eventually returns `null` for the deleted topic's partition.
