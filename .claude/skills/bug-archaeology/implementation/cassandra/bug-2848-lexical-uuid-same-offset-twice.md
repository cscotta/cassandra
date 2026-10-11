# Bug 2848: LexicalUUIDType.getUUID reads same offset for both UUID halves

**Commit**: 9b5c44caf3 · **Ticket**: n/a

## Detection Rule
When constructing a `UUID` from a `ByteBuffer` by reading two `long` values, the second `getLong` call must read at offset `+8`, not at the same position as the first; reading both from the same index produces a UUID where both halves are identical.

## Code Smell
```java
return new UUID(bytes.getLong(bytes.position()+bytes.arrayOffset()),
                bytes.getLong(bytes.position()+bytes.arrayOffset()));
```
Both `getLong` calls use `bytes.position()+bytes.arrayOffset()`, so `mostSigBits == leastSigBits`, producing malformed UUIDs with wrong ordering and comparisons.

## Tags
- scope: cassandra
- category: off-by-one
- code-pattern: bytebuffer-same-offset-twice
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause
The local `getUUID` helper in `LexicalUUIDType` read both the most-significant and least-significant 8 bytes from the same `ByteBuffer` position. The second read should be at position+8. This caused UUID comparisons and string representations to be incorrect. The fix inlines into `UUIDGen.getUUID` which correctly calls `getLong` twice with sequential positions via `ByteBuffer.getLong()` (which advances the position) or uses explicit offset arithmetic.

## Example
```java
// BEFORE (in LexicalUUIDType)
static UUID getUUID(ByteBuffer bytes) {
    return new UUID(
        bytes.getLong(bytes.position() + bytes.arrayOffset()),
        bytes.getLong(bytes.position() + bytes.arrayOffset())); // same offset!
}

// AFTER (in UUIDGen)
public static UUID getUUID(ByteBuffer raw) {
    // reads position, then position+8
    long most = raw.getLong(raw.position() + raw.arrayOffset());
    long least = raw.getLong(raw.position() + raw.arrayOffset() + 8);
    return new UUID(most, least);
}
```
