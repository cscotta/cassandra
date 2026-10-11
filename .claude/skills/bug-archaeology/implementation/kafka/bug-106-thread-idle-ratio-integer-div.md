**Commit**: 1c9190d6b1 · **Ticket**: KAFKA-18807
**Subsystem**: coordinator-common / MultiThreadedEventProcessor
**Category**: Wrong constant / default value

**Root cause**
`MultiThreadedEventProcessor` records the idle time for each coordinator thread by dividing the raw `idleTimeMs` by the number of threads using integer division: `idleTimeMs / threads.size()`. When `threads.size() > 1`, integer truncation discards the fractional part, causing the reported idle ratio to be systematically lower than the true value. The fix converts both operands to `double` before the division and changes the interface signature from `long` to `double`.

**Before/After**
```java
// Before — integer division loses fractional idle time
long idleTimeMs = idleEndTimeMs - idleStartTimeMs;
metrics.recordThreadIdleTime(idleTimeMs / threads.size());

// After — floating-point division preserves precision
metrics.recordThreadIdleTime((double) idleTimeMs / (double) threads.size());
```

**How to spot**
Whenever a measurement is divided by a count before being passed to a metric sink, verify that at least one operand is a floating-point type. Java integer division silently truncates; with many threads and small idle windows the reported value can be zero even when threads are mostly idle.

**Test**
`MultiThreadedEventProcessorTest` — creates a processor with multiple threads and a mock idle window, captures the recorded idle time, and asserts the per-thread value equals `100.0` (not `100`), with a final idle-ratio check of `1.0`.
