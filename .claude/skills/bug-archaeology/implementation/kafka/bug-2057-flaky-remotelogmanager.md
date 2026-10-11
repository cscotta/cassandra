**Commit**: 9c0fe85605 · **Ticket**: KAFKA-17779
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Kamal Chandraprakash <kamal.chandraprakash@gmail.com>

**Before/After**
```java
// Before
        LogSegment logSegment = mockLogSegment(50L, timestamp, null);
        LogSegment logSegment1 = mockLogSegment(100L, timestamp + 1, expectedLocalResult);
        when(mockLog.logSegments()).thenReturn(Arrays.asList(logSegment, logSegment1));
        when(mockLog.logSegments()).thenReturn(Collections.singletonList(logSegment1));
        when(logSegment1.baseOffset()).thenReturn(101L);

// After
import java.util.concurrent.atomic.AtomicInteger;
        LogSegment logSegmentBaseOffset50 = mockLogSegment(50L, timestamp, null);
        LogSegment logSegmentBaseOffset100 = mockLogSegment(100L, timestamp + 1, expectedLocalResult);
        LogSegment logSegmentBaseOffset101 = mockLogSegment(101L, timestamp + 1, expectedLocalResult);

        // Constants representing the states of local log segments
        final int twoSegmentsBaseOffsets50and100 = 0;
        final int oneSegmentBaseOffset100 = 1;
        final int oneSegmentBaseOffset101 = 2;

        AtomicInteger localLogOffsetState = new AtomicInteger(twoSegmentsBaseOffsets50and100);

```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17779: fix flaky remotelogmanager test (#17724).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
