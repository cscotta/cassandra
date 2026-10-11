# Bug 1252: Streaming Drops SSTable Ref on Stream Error, Causing Ref Leak

**Commit**: 9f7ab09f73 · **Ticket**: CASSANDRA-9295
**Subsystem**: src/java/org/apache/cassandra/streaming/
**Category**: Resource leak

---

## Root Cause

`SSTableStreamingSections` stored both an `SSTableReader` direct reference and a `Ref<SSTableReader>`. When a streaming error occurred, the `Ref` was not always released because the release path operated on the raw `SSTableReader` instead of going through the `Ref`. Additionally, `getSSTableSectionsForRanges` did not filter out early-opened (non-canonical) SSTables, so it could stream an SSTable that was in a transient half-open state, causing CRC validation failures. The fix removes the redundant direct field, derives the reader exclusively from `ref.get()`, and introduces an idempotent `complete()` method on `OutgoingFileMessage` that guards the single `ref.release()` call so double-release is impossible.

### Before / After

```java
// BEFORE – duplicate field; ref not always released on error path
public static class SSTableStreamingSections {
    public final SSTableReader sstable;   // raw reference
    public final Ref<SSTableReader> ref;
    ...
}
```

```java
// AFTER – ref is the only handle; reader obtained on demand
public static class SSTableStreamingSections {
    public final Ref<SSTableReader> ref;  // sole owner
    ...
}
```

---

## How to Spot

When a class holds both a raw object reference and a reference-counted `Ref` to the same object, the raw pointer is typically unnecessary and creates a dual-ownership confusion. Code review should flag any `Ref` field that is accompanied by a naked field of the same underlying type, then verify that every exit path (normal completion, cancellation, exception) calls `ref.release()` exactly once.

---

## Test

Existing `StreamTransferTaskTest` and `StreamingTransferTest` updated to use the new API. No new behavioral test added for the error path specifically.
