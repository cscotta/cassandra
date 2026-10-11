**Commit**: 889c3d45bd · **Ticket**: KAFKA-19888

**Subsystem**: coordinator-common/src/main/java/org/apache/kafka/coordinator/common/runtime/KafkaMetricHistogram.java

**Category**: Missing null/bounds check

**Root cause**
`KafkaMetricHistogram` recorded wall-clock latencies directly into an `HdrHistogram`. When the system clock moved backwards (e.g. due to NTP adjustment), the computed duration could be negative, causing `HdrHistogram.recordValue` to throw `ArrayIndexOutOfBoundsException`. This exception propagated through the coordinator's request-processing path, making the affected partition's group coordinator unavailable until a leadership change or broker restart. The fix clamps the recorded value to a minimum of 0 using `Math.max`.

**Before**
```java
// Before
recorder.recordValue(
    Math.min(value, highestTrackableValue));
```

**After**
```java
// After
recorder.recordValue(
    Math.min(Math.max(value, 0), highestTrackableValue));
```

**How to spot**
In any code that records a wall-clock duration into `HdrHistogram`, check whether the duration is clamped to ≥ 0 before recording; absent this clamp, a backwards clock tick will throw `ArrayIndexOutOfBoundsException` at the histogram level.

**Test**
Call `recordValue` with `-1` and verify the histogram records `0` rather than throwing an exception.
