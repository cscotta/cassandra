# Bug 1167: Wrong Result from NIODataInputStream.readVInt() on Little-Endian Buffers

**Commit**: 85c6b2edb8 · **Ticket**: CASSANDRA-9865
**Subsystem**: io/util/NIODataInputStream.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`NIODataInputStream.readVInt()` used `ByteBuffer.getLong(position)` to read a raw 8-byte word from the buffer, then used bit-shifting to extract the variable-length integer value. `ByteBuffer.getLong` respects the buffer's byte order, so on a little-endian buffer it returns the bytes in reversed significance order. The shift arithmetic that followed assumed big-endian layout, meaning the most-significant byte was always at the lowest address. On any `ByteBuffer` whose order was set to `LITTLE_ENDIAN` (which includes the default NIO direct buffers on x86 when explicitly set), the decoded integer would be wrong. The fix adds a `Long.reverseBytes` call after reading when the buffer order is `LITTLE_ENDIAN`, restoring the assumed big-endian layout before the bit arithmetic.

### Before / After

```java
// BEFORE
long retval = buf.getLong(position);
buf.position(position + extraBytes);
```

```java
// AFTER
long retval = buf.getLong(position);
if (buf.order() == ByteOrder.LITTLE_ENDIAN)
    retval = Long.reverseBytes(retval);
buf.position(position + extraBytes);
```

---

## How to Spot

Whenever a `ByteBuffer.getLong` (or similar multi-byte read) feeds directly into bit-shift arithmetic without an endian check, the code is implicitly assuming big-endian layout. Reviewers should check whether any code path can supply a `LITTLE_ENDIAN` buffer; if so, a `Long.reverseBytes` normalization step or an explicit `order(ByteOrder.BIG_ENDIAN)` assertion is required before the arithmetic.

---

## Test

No test added.
