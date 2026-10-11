**Commit**: bff1602df3 · **Ticket**: KAFKA-19280
**Subsystem**: storage/src/main/java/org/apache/kafka/storage/internals/log/UnifiedLog.java
**Category**: Race condition / missing synchronization

**Root cause**: `fetchLastStableOffsetMetadata()` and `lastStableOffset()` tested `firstUnstableOffsetMetadata.isPresent()` on the `volatile` field and then called `.get()` on the same field reference in a separate expression. Between the two reads another thread could set the field to `Optional.empty()`, causing the second read to return an empty `Optional` and `.get()` to throw `NoSuchElementException`. The fix snapshots the volatile into a local variable before the conditional, making both `isPresent()` and `get()` operate on the same immutable snapshot.

**Before/After**

```java
// Before – two separate reads of the volatile
if (firstUnstableOffsetMetadata.isPresent() &&
    firstUnstableOffsetMetadata.get().messageOffset < highWatermarkMetadata.messageOffset) {
    LogOffsetMetadata lom = firstUnstableOffsetMetadata.get();   // could be empty now

// After – single snapshot
Optional<LogOffsetMetadata> firstUnstableOffsetMetadataCopy = firstUnstableOffsetMetadata;
if (firstUnstableOffsetMetadataCopy.isPresent() &&
    firstUnstableOffsetMetadataCopy.get().messageOffset < highWatermarkMetadata.messageOffset) {
    LogOffsetMetadata lom = firstUnstableOffsetMetadataCopy.get();
```

**How to spot**: Any pattern of `volatile.isPresent()` followed by `volatile.get()` without an intervening lock is a TOCTOU bug. Treat every read of a `volatile` as potentially returning a different value from the previous read.

**Test**: No new test added for the race itself (it is timing-dependent). The change is verified by the existing log test suite and manual observation that `NoSuchElementException` no longer occurs in FETCH/TXN_OFFSET_COMMIT handling under load.
