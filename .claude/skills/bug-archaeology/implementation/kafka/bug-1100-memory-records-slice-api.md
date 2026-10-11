## Commit

2dd6126b5d — KAFKA-18855: Add slice API to MemoryRecords for ShareFetch tiered-storage support

## Ticket

KAFKA-18855

## Subsystem

Clients / record storage and share fetch

## Category

Missing null / bounds check

## Root cause

`Records` and `MemoryRecords` lacked a `slice(int position, int size)` method. `FileRecords` had its own `slice` implementation but it was not part of the `Records` interface contract, so code that held a `Records` reference could not slice without casting. For `ShareFetch` with tiered-storage support, the remote log storage emits `MemoryRecords` containing full batches, but the share fetch may have acquired only a subset of those batches. Without `MemoryRecords.slice`, the full buffer had to be transferred even when only a portion was needed, causing over-transfer of data and incorrect byte accounting. The fix adds `Records.slice` as an interface method and implements it in `MemoryRecords` using `ByteBuffer.duplicate().slice()` (zero-copy).

## Before / After

```java
// Before — no slice on Records interface; FileRecords had it privately
// MemoryRecords: no slice method
// Callers had to transfer entire buffer

// After — slice on Records interface, zero-copy MemoryRecords implementation
@Override
public Records slice(int position, int size) {
    int available = Math.min(size, buffer.limit() - position);
    ByteBuffer dup = buffer.duplicate();
    dup.position(position);
    dup.limit(position + available);
    return MemoryRecords.readableRecords(dup.slice());
}
```

## How to spot

Search for `Records` subtypes that do not implement `slice`. Any subtype used in a ShareFetch or tiered-storage fetch path that lacks `slice` will transfer more bytes than necessary and may return incorrect record counts to the share partition manager.

## Test

`MemoryRecordsTest` was extended with boundary cases for `slice`: position at start, middle, and end; size larger than available bytes; and negative position/size arguments.
