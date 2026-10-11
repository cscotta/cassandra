**Commit**: 98cdf97170 · **Ticket**: KAFKA-17240
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Make DelayedOperations#checkAndCompleteAll has chance to complete delayed ops even though there is a exception caused by one of delayed op. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    fetch.checkAndComplete(requestKey)
    produce.checkAndComplete(requestKey)
    deleteRecords.checkAndComplete(requestKey)
    try {
      delayedOperations.checkAndCompleteAll()
    } catch {
      case e: Exception =>
        error(s"Unexpected exception while completing delayed requests for ${topicPartition}", e)
    }
    when(delayedOperations.checkAndCompleteAll()).thenThrow(new RuntimeException("uh oh"))

// After
import org.slf4j.event.Level
    CoreUtils.swallow(() -> fetch.checkAndComplete(requestKey), fetch, Level.ERROR)
    CoreUtils.swallow(() -> produce.checkAndComplete(requestKey), produce, Level.ERROR)
    CoreUtils.swallow(() -> deleteRecords.checkAndComplete(requestKey), deleteRecords, Level.ERROR)
    delayedOperations.checkAndCompleteAll()
    val requestKey = TopicPartitionOperationKey(topicPartition)

    val produce = mock(classOf[DelayedOperationPurgatory[DelayedProduce]])
    when(produce.checkAndComplete(requestKey)).thenThrow(new RuntimeException("uh oh"))

    val fetch = mock(classOf[DelayedOperationPurgatory[DelayedFetch]])
    when(fetch.checkAndComplete(requestKey)).thenThrow(new RuntimeException("uh oh"))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17240 try to complete delyed ops for all purgatories even though one of delayed op throws exception (#16777).

**Test**
Test modified in PartitionTest.scala covering the corrected behaviour.
