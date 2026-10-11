# Bug 0088: WaitingOn Deserialization Corrupts Bit-Sets Due to ByteBuffer Position Advance

**Commit**: 266986b5a2 · **Ticket**: n/a
**Subsystem**: service/accord/serializers — WaitingOnSerializer
**Category**: Wrong serialization / deserialization

---

## Root Cause

`WaitingOnSerializer.deserialize(ByteBuffer)` read three consecutive `ImmutableBitSet` values from a `ByteBuffer` by calling `getLong()` (the relative-position variant) inside an inner helper that received no position parameter. However, the outer `deserialize` method passed the same `ByteBuffer` instance to three consecutive calls to the helper without advancing the buffer's position between calls, so all three bit-sets were decoded starting from the same offset. The fix switches to the absolute-position `getLong(int position)` variant and manually tracks the current read position, removing the dependency on the buffer's mutable position state.

### Before / After

```java
// BEFORE
ImmutableBitSet waitingOnCommit = deserialize(length, in);   // reads from position
ImmutableBitSet waitingOnApply  = deserialize(length, in);   // re-reads same position!
ImmutableBitSet appliedOrInvalidated = deserialize(length, in);

private static ImmutableBitSet deserialize(int length, ByteBuffer in) {
    long[] bits = new long[length];
    for (int i = 0; i < length; ++i)
        bits[i] = in.getLong();   // advances position side-effect relied on
    ...
}
```

```java
// AFTER
int position = in.position();
ImmutableBitSet waitingOnCommit = deserialize(position, length, in);
position += length * 8;
ImmutableBitSet waitingOnApply = deserialize(position, length, in);
position += length * 8;
ImmutableBitSet appliedOrInvalidated = deserialize(position, length, in);

private static ImmutableBitSet deserialize(int position, int length, ByteBuffer in) {
    long[] bits = new long[length];
    for (int i = 0; i < length; ++i) {
        bits[i] = in.getLong(position);   // absolute read, no side-effect
        position += 8;
    }
    ...
}
```

---

## How to Spot

When a `ByteBuffer` is passed through multiple sequential decode calls, verify whether each call uses relative reads (which advance the buffer's internal position as a side-effect) or absolute reads (which do not). If relative reads are used, every subsequent call must receive the *same* buffer instance in an already-advanced state; if absolute reads are used, the caller must manually track and advance the position. Mixing the two conventions—or passing the unadvanced buffer to all three calls as happened here—produces silently wrong data with no exception thrown.

---

## Test

No test added; the fix is contained in the serialization helper alone.
