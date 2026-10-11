# Bug 2272: ByteBuffer.rewind() Used Instead of flip() After Writing Key

**Commit**: 197511f0b1 · **Ticket**: CASSANDRA-4534

## Detection Rule

When a ByteBuffer is filled with `put()` and then handed to a reader, confirm that `flip()` (not `rewind()`) is called to set the limit correctly; `rewind()` only resets position but leaves limit at capacity, exposing uninitialized bytes.

## Code Smell

`key.rewind()` follows `key.put(...)` — `rewind()` resets `position` to 0 but does not adjust `limit`, so the buffer appears to contain `capacity` bytes rather than only the bytes just written.

## Tags

- scope: java
- category: wrong-serialization
- code-pattern: bytebuffer-rewind-instead-of-flip
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause

The Hadoop `ColumnFamilyRecordReader` old-style `mapred` API filled a `ByteBuffer` with `key.put(this.getCurrentKey())` and then called `key.rewind()`. `rewind()` sets `position=0` but leaves `limit` at the buffer capacity, so downstream readers see extra zero bytes beyond the actual key. `flip()` correctly sets `limit=position` before resetting `position=0`, exposing only the valid key bytes.

## Example

```java
// BEFORE
key.clear();
key.put(this.getCurrentKey());
key.rewind();  // limit stays at capacity; exposes garbage bytes
```

```java
// AFTER
key.clear();
key.put(this.getCurrentKey());
key.flip();    // limit = bytes written; position = 0
```
