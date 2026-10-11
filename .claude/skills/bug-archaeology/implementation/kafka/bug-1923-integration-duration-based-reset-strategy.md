**Commit**: f87c04854b · **Ticket**: KAFKA-18013
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Add new test case to PlaintextConsumerFetchTest

Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
                            startingTimestamp: Long = System.currentTimeMillis()): Seq[ProducerRecord[Array[Byte], Array[Byte]]] = {
      val timestamp = startingTimestamp + i.toLong
                                        maxPollRecords: Int = Int.MaxValue): Unit = {
        val timestamp = startingTimestamp + i
        assertEquals(timestamp, record.timestamp)
import java.time.Duration

// After
                            startingTimestamp: Long = System.currentTimeMillis(),
                            timestampIncrement: Long = -1L): Seq[ProducerRecord[Array[Byte], Array[Byte]]] = {
      val timestamp = if (timestampIncrement > 0)
        startingTimestamp + i.toLong * timestampIncrement
      else
        startingTimestamp + i.toLong
                                        maxPollRecords: Int = Int.MaxValue,
                                        timestampIncrement: Long = -1L): Unit = {
        if (timestampIncrement > 0) {
          val timestamp = startingTimestamp + i * timestampIncrement
          assertEquals(timestamp, record.timestamp)
        } else {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18013: add integration test for duration based reset strategy in kafkaconsumer (#18008).

**Test**
Test modified in AbstractConsumerTest.scala covering the corrected behaviour.
