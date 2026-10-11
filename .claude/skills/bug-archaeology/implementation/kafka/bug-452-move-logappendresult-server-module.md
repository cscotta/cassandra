**Commit**: ff1b9b64ab · **Ticket**: KAFKA-19915
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Move LogAppendResult as a record to Java

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    partitionResult.info.lastOffset + 1
import org.apache.kafka.server.{ActionQueue, DelayedActionQueue, HostedPartition, common}
/*
 * Result metadata of a log append operation on the log
 */
case class LogAppendResult(info: LogAppendInfo,
                           exception: Option[Throwable],
                           hasCustomErrorMessage: Boolean) {
  def error: Errors = exception match {
    case None => Errors.NONE
    case Some(e) => Errors.forException(e)
  }

// After
    partitionResult.logAppendSummary.lastOffset + 1
import org.apache.kafka.server.LogAppendResult.LogAppendSummary
import org.apache.kafka.server.{ActionQueue, DelayedActionQueue, HostedPartition, LogAppendResult, common}
      k -> v.logAppendSummary().recordValidationStats()
          new TopicIdPartition(topicIds.getOrElse(topicPartition.topic(), Uuid.ZERO_UUID), topicPartition) -> new LogAppendResult(
            LogAppendSummary.fromAppendInfo(LogAppendInfo.UNKNOWN_LOG_APPEND_INFO),
            Optional.ofNullable(customException.getOrElse(error.exception)),
            customException.isDefined
        result.logAppendSummary.lastOffset + 1, // required offset
          result.logAppendSummary.firstOffset,
          result.logAppendSummary.logAppendTime,
          result.logAppendSummary.logStartOffset,
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19915: [1/3] move logappendresult to server module (#21410).

**Test**
Test modified in CoordinatorPartitionWriterTest.scala covering the corrected behaviour.
