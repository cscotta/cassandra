# Bug 0370: Streaming start timestamp sentinel not recognized when field is primitive long

**Commit**: b6de7acdb23704ef5165fb674d6814060f60e754 · **Ticket**: #15298

## Detection Rule

When a method accepts a primitive numeric parameter as a default-sentinel (e.g., `Long.MIN_VALUE` meaning "not set") but the code checks for a boxed-null (`== null`) to detect that sentinel, verify that the primitive field is never null and the sentinel check matches the actual default value produced by the upstream source.

## Code Smell

A `static` helper method declared with a boxed `Long` parameter that is always called with a primitive `long` field, where the method body checks `if (fromTimestamp == null)` to detect "no timestamp configured," while the field was assigned from an API returning a primitive `long` with default `Long.MIN_VALUE`.

## Tags

- scope: java
- category: wrong-constant
- code-pattern: null-check-on-primitive-sentinel
- severity: silent-wrong-result
- subsystem: spark/streaming/offset-planning

## Root Cause

`SparkReadConf.streamFromTimestamp()` returns a primitive `long` defaulting to `Long.MIN_VALUE` when the user has not set `STREAM_FROM_TIMESTAMP`. The old `determineStartingOffset(Table, Long)` accepted a boxed `Long` and checked `fromTimestamp == null` to detect the "no timestamp" case, but the field passed in was always a primitive `long` (auto-boxed, never null). The null branch was therefore unreachable; when no timestamp was configured the code fell through to `SnapshotUtil.oldestAncestorAfter(table, Long.MIN_VALUE)` instead of starting from the oldest snapshot directly.

## Example

```java
// BEFORE — null check unreachable; Long.MIN_VALUE passes through to oldestAncestorAfter()
private static StreamingOffset determineStartingOffset(Table table, Long fromTimestamp) {
    if (fromTimestamp == null) {  // never true: caller passes primitive long
        return new StreamingOffset(SnapshotUtil.oldestAncestor(table).snapshotId(), 0, false);
    }
    // falls through with Long.MIN_VALUE, incorrect behavior
    return new StreamingOffset(SnapshotUtil.oldestAncestorAfter(table, fromTimestamp).snapshotId(), 0, false);
}
```

```java
// AFTER — checks the actual sentinel value used by SparkReadConf.streamFromTimestamp()
static StreamingOffset determineStartingOffset(Table table, long fromTimestamp) {
    if (fromTimestamp == Long.MIN_VALUE) {
        // start from the oldest snapshot, since default value is MIN_VALUE
        return new StreamingOffset(SnapshotUtil.oldestAncestor(table).snapshotId(), 0, false);
    }
    // proceeds with a real user-supplied timestamp
    return new StreamingOffset(SnapshotUtil.oldestAncestorAfter(table, fromTimestamp).snapshotId(), 0, false);
}
```
