# Bug 2516: DecimalType ByteBuffer consumed by getInt() before composing BigInteger

**Commit**: b2dcd3e5a5 · **Ticket**: CASSANDRA-3421

## Detection Rule
When composing a type from a shared `ByteBuffer`, call `bytes.duplicate()` before any `get*()` call to avoid advancing the original buffer's position and corrupting subsequent reads.

## Code Smell
`int scale = bytes.getInt()` followed by `bytes.get(bibytes, 0, bytes.remaining())` — `getInt()` advances the buffer position, so the later `remaining()` call is correct, but the original `ByteBuffer` passed in has its position mutated as a side-effect, breaking any caller that reuses it. Also `getInt()` on an un-duplicated buffer makes `compare()` advance the arguments it receives.

## Tags
- scope: java
- category: wrong-serialization
- code-pattern: bytebuffer-position-side-effect
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`DecimalType.compare()` called `compose(bb0).compareTo(compose(bb1))` to compare two `BigDecimal` values. Inside `compose()`, `bytes.getInt()` mutated the position of the `ByteBuffer` passed in. Because `compare()` passes the original (non-duplicated) buffer, the first call to `compose(bb0)` advances `bb0`'s position past the scale, and the second invocation of the same comparison (e.g. in a sort) would read garbage. The fix calls `bytes.duplicate()` at the start of `compose()` to work on a private view.

## Example
```java
// BEFORE
public BigDecimal compose(ByteBuffer bytes) {
    int scale = bytes.getInt();           // mutates caller's buffer
    byte[] bibytes = new byte[bytes.remaining()];
    bytes.get(bibytes, 0, bytes.remaining());
    return new BigDecimal(new BigInteger(bibytes), scale);
}
```
```java
// AFTER
public BigDecimal compose(ByteBuffer bytes) {
    bytes = bytes.duplicate();            // safe: caller's position unchanged
    int scale = bytes.getInt();
    byte[] bibytes = new byte[bytes.remaining()];
    bytes.get(bibytes);
    return new BigDecimal(new BigInteger(bibytes), scale);
}
```
