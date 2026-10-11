**Commit**: 385b70d2f8 · **Ticket**: KAFKA-18475

**Subsystem**: core/src/test/scala/integration/kafka/api/BaseProducerSendTest.scala

**Category**: Race condition/missing synchronization

**Root cause**
`testCloseWithZeroTimeoutFromCallerThread` assigned a `TopicPartition` to a consumer and then asserted that `consumer.poll()` returned zero records after a force-close of the producer. A race between `RecordAccumulator.close()` and `RecordAccumulator.batchReady()` allowed one additional `Sender.runOnce()` to fire after `forceClose`, potentially delivering records to the broker before the producer was torn down. The test assertion `assertEquals(0, consumer.poll(...).count())` was therefore inherently racy and could not be made reliable without a lock. The fix removes the consumer assignment and the racy zero-count assertion, keeping only the already-reliable assertion that producer futures complete exceptionally.

**Before**
```scala
// Before
consumer.assign(java.util.List.of(new TopicPartition(topic, partition)))
// ...
assertEquals(0, consumer.poll(Duration.ofMillis(50L)).count,
  "Fetch response should have no message returned.")
```

**After**
```scala
// After: consumer assign and racy poll assertion removed
// only future.get() exception assertion retained
```

**How to spot**
Any test that calls `consumer.poll()` immediately after a `producer.close(Duration.ZERO)` and asserts zero records received is susceptible to this race; force-close only guarantees that in-flight batches are failed on the producer side, not that the broker never receives them.

**Test**
Run `testCloseWithZeroTimeoutFromCallerThread` 500 times after the fix and confirm no flaky failures due to the consumer receiving unexpected records.
