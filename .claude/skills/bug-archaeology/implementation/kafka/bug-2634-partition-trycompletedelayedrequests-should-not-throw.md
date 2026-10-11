**Commit**: 15fa241cea · **Ticket**: KAFKA-17234
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
`Partition.tryCompleteDelayedRequests` should not throw since the
contexts it is called from are not expecting it, and the exceptions are
not related to those contexts. Reviewers: Justine Olshan <jolshan@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  private def tryCompleteDelayedRequests(): Unit = delayedOperations.checkAndCompleteAll()

// After
  def tryCompleteDelayedRequests(): Unit = {
    try {
      delayedOperations.checkAndCompleteAll()
    } catch {
      case e: Exception =>
        error(s"Unexpected exception while completing delayed requests for ${topicPartition}", e)
    }
  }

  @Test
  def tryCompleteDelayedRequestsCatchesExceptions(): Unit = {
    when(delayedOperations.checkAndCompleteAll()).thenThrow(new RuntimeException("uh oh"))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17234: partition.trycompletedelayedrequests should not throw (#16760).

**Test**
Test modified in PartitionTest.scala covering the corrected behaviour.
