**Commit**: 2b6e868538 · **Ticket**: KAFKA-18784
**Subsystem**: core/src/test/scala/integration/kafka/api/ConsumerWithLegacyMessageFormatIntegrationTest.scala
**Category**: Wrong serialization / deserialization

**Root cause**
After Apache Kafka 4.0 removed ZooKeeper mode, the old message formats (v0, v1) could no longer be sent via a normal `KafkaProducer`. The integration test `ConsumerWithLegacyMessageFormatIntegrationTest` was silently using v2 format for all topics, meaning the "legacy format" scenarios were not actually testing v0/v1 behaviour. The test was also making wrong assertions: for example, it expected `offsetsForTimes` to return a non-null result for a v0 topic (v0 has no timestamp, so the result should be `null`). The fix rewrites the test to append legacy records directly through `ReplicaManager`/`UnifiedLog.appendAsLeaderWithRecordVersion`, and corrects the assertions to reflect actual v0/v1 semantics.

**Before/After**
```scala
// Before: all topics sent with normal producer (v2 format), wrong assertions
sendRecords(producer, numRecords = 100, tp, startingTimestamp = 0)
// ...
val timestampTopic2P0 = timestampOffsets.get(new TopicPartition(topic2, 0))
assertEquals(40, timestampTopic2P0.offset)  // wrong: v0 has no timestamp → null

// After: legacy formats written directly; correct assertions
appendLegacyRecords(100, t2p0, 0, RecordBatch.MAGIC_VALUE_V0)
// ...
val timestampTopic2P0 = timestampOffsets.get(t2p0)
assertNull(timestampTopic2P0)  // correct: v0 has no timestamp
```

**How to spot**
When a feature is removed from the production path but tests are supposed to cover backward-compatible behaviour, check whether the test setup actually exercises the old code path. A test that works correctly but tests the wrong thing (because the setup silently upgraded the format) gives false confidence.

**Test**
The test was rewritten in this commit. It now appends v0 and v1 records via `UnifiedLog.appendAsLeaderWithRecordVersion` and asserts null for v0 `offsetsForTimes`, empty `leaderEpoch` for v1, and correct offset/timestamp for v2 format topics.
