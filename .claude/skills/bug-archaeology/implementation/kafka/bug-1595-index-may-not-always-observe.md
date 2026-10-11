**Commit**: e124d3975b · **Ticket**: KAFKA-806
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Currently, each log.append() will add at most 1 index entry, even when the appended data is larger than log.index.interval.bytes. One potential issue is that if a follower restarts after being down for a long time, it may fetch data much bigger than log.index.interval.bytes at a time. This means that fewer index entries are created, which can increase the fetch time from the consumers. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
        dest.append(result.maxOffset, result.maxTimestamp, result.shallowOffsetOfMaxTimestamp(), retained)
            appendInfo.setShallowOffsetOfMaxTimestamp(validateAndOffsetAssignResult.shallowOffsetOfMaxTimestamp)
              localLog.append(appendInfo.lastOffset, appendInfo.maxTimestamp, appendInfo.shallowOffsetOfMaxTimestamp, validRecords)
    new LogAppendInfo(firstOffset, lastOffset, lastLeaderEpochOpt, maxTimestamp, shallowOffsetOfMaxTimestamp,
    seg1.append(1, 1000L, 1, MemoryRecords.withRecords(1, Compression.NONE, new SimpleRecord("one".getBytes)))
    seg2.append(2, 2000L, 1, MemoryRecords.withRecords(2, Compression.NONE, new SimpleRecord("two".getBytes)))
      shallowOffsetOfMaxTimestamp,
      -1L,

// After
        dest.append(result.maxOffset, retained)
              localLog.append(appendInfo.lastOffset, validRecords)
    new LogAppendInfo(firstOffset, lastOffset, lastLeaderEpochOpt, maxTimestamp,
    seg1.append(1, MemoryRecords.withRecords(1, Compression.NONE, new SimpleRecord(1000L, "one".getBytes)))
    seg2.append(2, MemoryRecords.withRecords(2, Compression.NONE, new SimpleRecord(2000L, "two".getBytes)))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-806: index may not always observe log.index.interval.bytes (#18012).

**Test**
Test modified in UnifiedLogTest.scala covering the corrected behaviour.
