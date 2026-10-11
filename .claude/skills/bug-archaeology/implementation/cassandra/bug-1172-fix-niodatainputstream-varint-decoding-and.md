# Bug 1172: NIODataInputStream Varint Decoding Corrupts Data and Gives Wrong EOF

**Commit**: c4c9eaeb13 · **Ticket**: CASSANDRA-9863
**Subsystem**: src/java/org/apache/cassandra/io/util/NIODataInputStream.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`NIODataInputStream` was designed for streaming reads from a `ReadableByteChannel`, using an internal 9-byte sliding window that refills from the channel as bytes are consumed. When wrapping a fixed `ByteBuffer` (no channel), two separate bugs appeared. First, `readUnsignedVInt` called `prepareReadPaddedPrimitive`, which temporarily widened `buf.limit()` beyond the real data to create padding, then shrank it back — but this manipulation could expose or overwrite bytes outside the encoded integer, producing wrong decoded values when fewer than 9 bytes remained in the buffer. Second, `readMinimum` accepted a two-argument form where `attempt` (how many bytes to try for) could be larger than `require` (how many were truly needed), so an EOF was silently swallowed when between `require` and `attempt` bytes were available, instead of throwing `EOFException`. The fix introduces `DataInputBuffer`, a thin subclass that overrides `readNext()` to return `-1` immediately, making it safe for fixed-buffer use, while `readUnsignedVInt` is rewritten to delegate to the slow path (`VIntCoding.readUnsignedVInt`) whenever fewer than 9 bytes remain, entirely eliminating the padding trick.

### Before / After

```java
// BEFORE: padding manipulation could corrupt buffer limits
int limitToSet = prepareReadPaddedPrimitive(8);
int position = buf.position();
int extraBytes = VIntCoding.numberOfExtraBytesToRead(firstByte);
long retval = buf.getLong(position);
buf.position(position + extraBytes);
buf.limit(limitToSet); // restoring limit that was temporarily extended
```

```java
// AFTER: slow path used when buffer is too small; no padding needed
if (buf.remaining() < 9)
    return VIntCoding.readUnsignedVInt(this);

byte firstByte = buf.get();
if (firstByte >= 0) return firstByte;

int extraBytes = VIntCoding.numberOfExtraBytesToRead(firstByte);
int position = buf.position();
long retval = buf.getLong(position);
buf.position(position + extraBytes);
retval >>>= 64 - extraBytes * 8;
```

---

## How to Spot

Any stream class that shares a single code path for both channel-backed and buffer-backed modes is prone to this class of bug. Look for limit or position manipulations that serve as "padding" to satisfy an algorithm that requires a minimum number of bytes: if the backing storage is finite and already positioned correctly, that padding is unnecessary and dangerous. A clean design keeps the two modes separate (via subclassing or a strategy), as done here with `DataInputBuffer`.

---

## Test

Three new unit tests are added to `NIODataInputStreamTest`: `testVIntRemainingBytes` (checks correct decoding when fewer than 9 bytes follow the varint in the buffer), `testVIntSmallBuffer` (checks correct decoding followed by an `EOFException` on exhaustion), and `testVIntTruncationEOF` (checks that a truncated varint correctly throws `EOFException`).
