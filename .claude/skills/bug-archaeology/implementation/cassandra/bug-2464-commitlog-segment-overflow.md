# Bug 2464: CommitLog segment overflows when writing end-of-segment marker without checking remaining space

**Commit**: 48f5d57164 · **Ticket**: CASSANDRA-3615

## Detection Rule

When a fixed-size marker is written to a byte buffer without first checking that `buffer.remaining() >= markerSize`, a `BufferOverflowException` will be thrown if the buffer is exactly full.

## Code Smell

Unconditional `buffer.putInt(...)` followed by a position rewind with no guard on `buffer.remaining()`. The pattern "write sentinel then rewind" is only safe if the buffer has room for the sentinel.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: buffer-overflow-no-remaining-check
- severity: crash
- subsystem: commitlog-cdc

## Root Cause

After writing a row to the commit log segment, the code unconditionally wrote the 4-byte `END_OF_SEGMENT_MARKER` to mark where the segment ends. If the row exactly consumed the remaining buffer space, no room was left for the marker, causing a `BufferOverflowException`. The fix wraps the marker write in a check for `buffer.remaining() >= 4`.

## Example

```java
// BEFORE
buffer.putInt(CommitLog.END_OF_SEGMENT_MARKER);
buffer.position(buffer.position() - CommitLog.END_OF_SEGMENT_MARKER_SIZE);
```

```java
// AFTER
if (buffer.remaining() >= 4)
{
    buffer.putInt(CommitLog.END_OF_SEGMENT_MARKER);
    buffer.position(buffer.position() - CommitLog.END_OF_SEGMENT_MARKER_SIZE);
}
```
