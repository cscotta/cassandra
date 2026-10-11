**Commit**: c293afc958 · **Ticket**: KAFKA-19614
**Subsystem**: jmh-benchmarks/src/main/java/org/apache/kafka/jmh/log/TestLinearWriteSpeed.java
**Category**: Logic error in condition

**Root cause**
`TestLinearWriteSpeed` created a single `MemoryRecords` batch object and reused it across every append iteration inside the benchmark loop. Because `appendAsLeader()` updates the batch's `baseOffset` in place, subsequent iterations passed batches with ever-increasing base offsets rather than starting from 0 each time. This caused the log layer to reject or mis-index records. The fix creates a fresh `MemoryRecords` object for each iteration, ensuring the base offset always starts at 0.

**Before/After**
```java
// BEFORE – single batch reused; baseOffset accumulates
MemoryRecords records = createRecords(messageSize, bufferSize);
while (bytesWritten < bytesToWrite) {
    log.appendAsLeader(records, leaderEpoch);   // baseOffset grows each call
    bytesWritten += bufferSize;
}

// AFTER – new batch per append
while (bytesWritten < bytesToWrite) {
    MemoryRecords records = createRecords(messageSize, bufferSize);
    log.appendAsLeader(records, leaderEpoch);
    bytesWritten += bufferSize;
}
```

**How to spot**
Any benchmark or test that calls a stateful-write API in a loop while reusing the same mutable batch/buffer object is suspect. Grep for `MemoryRecords` or `RecordBatch` instances created outside the loop body and passed to `appendAsLeader`/`appendAsFollower` inside the loop.

**Test**
No dedicated unit test added; the commit is a benchmark fix. The commit message notes that profiling confirmed `appendAsLeader` dominates runtime and that rebuilding `MemoryRecords` has negligible overhead.
