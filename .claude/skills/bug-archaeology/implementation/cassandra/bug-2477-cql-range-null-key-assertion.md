# Bug 2477: CQL range scan asserts when key range is unbounded — null decorated key fails compareTo

**Commit**: 2ffe87e9d2 · **Ticket**: CASSANDRA-3574

## Detection Rule

When `null` is passed to a partitioner's `decorateKey()` instead of using the dedicated `RowPosition.forKey(null, p)` sentinel, the resulting `DecoratedKey` will cause assertion errors or NPEs when compared with range bounds.

## Code Smell

`p.decorateKey(new Term().getByteBuffer())` used to represent an unbounded key range instead of `RowPosition.forKey(null, p)`. The `new Term().getByteBuffer()` call produces an empty ByteBuffer, not the minimum/maximum sentinel value.

## Tags

- scope: cassandra
- category: wrong-constant
- code-pattern: empty-buffer-instead-of-sentinel
- severity: crash
- subsystem: cql-query

## Root Cause

In `QueryProcessor`, when a CQL SELECT had no explicit key start/finish, the code used `new Term().getByteBuffer()` (an empty ByteBuffer) to construct a `DecoratedKey` boundary. `p.decorateKey(emptyBuffer)` creates a key that compares incorrectly to min/max sentinels, triggering assertion errors. The fix uses `null` as input and `RowPosition.forKey(null, p)` which properly returns the minimum or maximum position.

## Example

```java
// BEFORE
ByteBuffer startKeyBytes = (select.getKeyStart() != null)
    ? select.getKeyStart().getByteBuffer(keyType)
    : (new Term()).getByteBuffer();
RowPosition startKey = p.decorateKey(startKeyBytes);
```

```java
// AFTER
ByteBuffer startKeyBytes = (select.getKeyStart() != null)
    ? select.getKeyStart().getByteBuffer(keyType)
    : null;
RowPosition startKey = RowPosition.forKey(startKeyBytes, p);
```
