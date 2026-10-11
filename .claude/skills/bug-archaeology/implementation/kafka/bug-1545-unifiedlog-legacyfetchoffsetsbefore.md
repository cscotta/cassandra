**Commit**: 66868fc1fa · **Ticket**: KAFKA-18620
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
  def legacyFetchOffsetsBefore(timestamp: Long, maxNumOffsets: Int): Seq[Long] = {
    // Cache to avoid race conditions. `toBuffer` is faster than most alternatives and provides
    // constant time access while being safe to use with concurrent collections unlike `toArray`.
    val allSegments = logSegments.asScala.toBuffer
    val lastSegmentHasSize = allSegments.last.size > 0

    val offsetTimeArray =
      if (lastSegmentHasSize)
        new Array[(Long, Long)](allSegments.length + 1)
      else
        new Array[(Long, Long)](allSegments.length)


// After
import org.apache.kafka.storage.internals.log.{LogStartOffsetIncrementReason, OffsetResultHolder}
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18620: remove unifiedlog#legacyfetchoffsetsbefore (#18686).

**Test**
Test modified in LogOffsetTest.scala covering the corrected behaviour.
