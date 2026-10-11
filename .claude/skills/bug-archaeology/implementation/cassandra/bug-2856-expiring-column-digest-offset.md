# Bug 2856: ExpiringColumn.updateDigest ignores ByteBuffer position and arrayOffset

**Commit**: e9b9baa54f · **Ticket**: CASSANDRA-1679

## Detection Rule
When calling `MessageDigest.update(ByteBuffer)` or `digest.update(bb.array())`, always pass the explicit `array(), position()+arrayOffset(), remaining()` form to avoid digesting extra bytes outside the buffer's valid range.

## Code Smell
`digest.update(name())` and `digest.update(value())` call the `update(ByteBuffer)` overload, which for a heap buffer may read from `position=0` to `limit`, ignoring `arrayOffset`. For sliced buffers this includes stale or unrelated bytes before the actual data, producing a wrong digest.

## Tags
- scope: cassandra
- category: wrong-serialization
- code-pattern: bytebuffer-digest-ignores-offset
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`Column.updateDigest` in the base class similarly needed fixing, and `ExpiringColumn` overrides it but had the same bug. The Java `MessageDigest.update(ByteBuffer)` documentation states it uses the buffer's current position and limit, but the wrapper here passed the raw ByteBuffer directly via a different overload path. The fix uses `digest.update(bb.array(), bb.position()+bb.arrayOffset(), bb.remaining())` for both `name` and `value`.

## Example
```java
// BEFORE
digest.update(name());
digest.update(value());

// AFTER
digest.update(name.array(), name.position()+name.arrayOffset(), name.remaining());
digest.update(value.array(), value.position()+value.arrayOffset(), value.remaining());
```
