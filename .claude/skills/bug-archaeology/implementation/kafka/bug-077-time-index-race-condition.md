**Commit**: 959021de59 · **Ticket**: KAFKA-19407
**Subsystem**: storage/src/main/java/org/apache/kafka/storage/internals/log/LogSegment.java
**Category**: Race condition / missing synchronization

**Root cause**: `readMaxTimestampAndOffsetSoFar()` used a non-atomic check-then-act pattern on the `volatile` field `maxTimestampAndOffsetSoFar`. Two threads could both observe `UNKNOWN` on the `if` line and then both call `timeIndex().lastEntry()`, writing the result to the volatile. The second write could store a stale or differently-ordered offset, causing a non-monotonic timestamp index entry. When the replication pipeline later tried to append to that index it received an `IllegalStateException` ("Attempt to append...") and replication halted. The fix wraps the lazy-initialisation block in a `synchronized` guard with a double-checked locking pattern.

**Before/After**

```java
// Before – TOCTOU on volatile field
public TimestampOffset readMaxTimestampAndOffsetSoFar() throws IOException {
    if (maxTimestampAndOffsetSoFar == TimestampOffset.UNKNOWN)
        maxTimestampAndOffsetSoFar = timeIndex().lastEntry();
    return maxTimestampAndOffsetSoFar;
}

// After – double-checked locking
synchronized (maxTimestampAndOffsetLock) {
    if (maxTimestampAndOffsetSoFar == TimestampOffset.UNKNOWN) {
        maxTimestampAndOffsetSoFar = timeIndex().lastEntry();
    }
}
```

**How to spot**: Any `if (volatileField == SENTINEL) volatileField = compute()` pattern without synchronization is a classic TOCTOU race. The symptom (non-monotonic offsets causing `IllegalStateException` during index append) only appears under concurrent access, making it hard to reproduce deterministically.

**Test**: Launches 16 threads that all call `maxTimestampSoFar()` concurrently on a freshly-created segment. Uses a Mockito spy on `TimeIndex` to verify `lastEntry()` is called exactly once over a 1-second stress window, confirming the initialisation is idempotent under concurrency.
