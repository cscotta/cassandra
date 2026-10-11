# Bug 2736: Large-row compaction size pass and write pass use different TTL expiry timestamps

**Commit**: 09a2667e7d · **Ticket**: CASSANDRA-2349

## Detection Rule
When a two-pass algorithm (size estimation then actual write) must treat expiry the same way, capture the reference time once and pass it explicitly to both passes.

## Code Smell
`ColumnSerializer.deserialize()` calls `System.currentTimeMillis()` each time; a size-estimation pass and a subsequent write pass executed seconds apart will disagree on which columns are expired, producing a size mismatch that corrupts large-row compaction output.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: inconsistent-timestamp-across-passes
- severity: data-loss
- subsystem: compaction

## Root Cause
Large-row compaction first estimates the output row size (to pre-allocate), then re-reads and writes columns. Both passes called `System.currentTimeMillis() / 1000` inside `ColumnSerializer.deserialize()` independently. If a TTL-expiring column's deadline fell between the two calls, the size pass counted it as live (larger size), but the write pass counted it as a tombstone (smaller payload), producing an under-filled or corrupt row segment. The fix adds a `deserialize(DataInput, int expireBefore)` overload and threads a single captured `expireBefore` value through both passes.

## Example
```java
// BEFORE — expiry time recaptured independently on each call
if ((int)(System.currentTimeMillis() / 1000) > expiration) {
    // treat as tombstone
}
```
```java
// AFTER — caller-supplied expireBefore is consistent across passes
public Column deserialize(DataInput dis, int expireBefore) throws IOException {
    ...
    if (expiration < expireBefore) {
        // treat as tombstone
    }
}
```
