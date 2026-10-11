**Commit**: 22d4248fba · **Ticket**: KAFKA-18694
**Subsystem**: coordinator-common/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Ken Huang <s7133700@gmail.com>, Christo Lolov <lolovc@amazon.com>

**Before/After**
```java
// Before
    private static class TaskResult<R> {
        final R result;
        final Throwable exception;

        TaskResult(
            R result,
            Throwable exception
        ) {
            this.result = result;
            this.exception = exception;
        }
    }

// After
    private record TaskResult<R>(R result, Throwable exception) { }
    record LoadSummary(long startTimeMs, long endTimeMs, long schedulerQueueTimeMs, long numRecords, long numBytes) { }
    private record Timestamped<T>(long timestamp, T value) { }
```

**How to spot**
When reviewing coordinator-common/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18694: migrate suitable classes to records in coordinator-common module (#18782).

**Test**
No test added.
