**Commit**: 4d4938cf1f · **Ticket**: KAFKA-19915
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Move LogDeleteRecordsResult to Java

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        LogDeleteRecordsResult(
          requestedOffset = convertedOffset,
          lowWatermark = lowWatermarkIfLeader)
import org.apache.kafka.server.{ActionQueue, DelayedActionQueue, HostedPartition, LogAppendResult, common}
case class LogDeleteRecordsResult(requestedOffset: Long, lowWatermark: Long, exception: Option[Throwable] = None) {
  def error: Errors = exception match {
    case None => Errors.NONE
    case Some(e) => Errors.forException(e)
  }
}

        (topicPartition, LogDeleteRecordsResult(-1L, -1L, Some(new InvalidTopicException(s"Cannot delete records of internal topic ${topicPartition.topic}"))))

// After
import org.apache.kafka.server.LogDeleteRecordsResult
        new LogDeleteRecordsResult(convertedOffset, lowWatermarkIfLeader, Optional.empty())
import org.apache.kafka.server.{ActionQueue, DelayedActionQueue, HostedPartition, LogAppendResult, LogDeleteRecordsResult, common}
        (topicPartition, new LogDeleteRecordsResult(-1L, -1L, Optional.of(new InvalidTopicException(s"Cannot delete records of internal topic ${topicPartition.topic}"))))
            (topicPartition, new LogDeleteRecordsResult(-1L, -1L, Optional.of(e)))
            (topicPartition, new LogDeleteRecordsResult(-1L, -1L, Optional.of(e)))
            (topicPartition, new LogDeleteRecordsResult(-1L, -1L, Optional.of(t)))
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding c
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19915: [3/3] move logdeleterecordsresult to server module (#21477).

**Test**
No test added.
