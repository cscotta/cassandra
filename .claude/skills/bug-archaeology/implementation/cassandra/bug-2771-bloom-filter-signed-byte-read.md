# Bug 2771: Bloom Filter Read Returns Signed Byte Causing False Negatives

**Commit**: 87ea405523 · **Ticket**: CASSANDRA-2165

## Detection Rule
When implementing `InputStream.read()` over a `ByteBuffer`, mask the returned byte with `0xFF` to ensure the value is in the range 0-255; returning the raw signed byte produces negative values that callers interpret as EOF (-1) or wrong data.

## Code Smell
`return copy.get()` in a custom `InputStream` wrapping a `ByteBuffer` returns a signed Java byte (range -128 to 127); `InputStream.read()` contract requires an unsigned value (0-255), so values >= 128 are incorrectly negative.

## Tags
- scope: cassandra
- category: wrong-serialization
- code-pattern: signed-byte-in-inputstream-read
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`ByteBufferUtil.inputStream` returned an `InputStream` whose single-byte `read()` method did `return copy.get()`. Java `byte` is signed, so bytes with high bits set (>= 128) became negative integers. The `InputStream` contract requires `read()` to return an unsigned byte (0-255) or -1 for EOF. The bloom filter deserializer interpreted these negative values as corruption or EOF, causing false negatives. The fix applies `& 0xFF` to ensure unsigned range.

## Example
```java
// BEFORE
public int read() throws IOException {
    if (!copy.hasRemaining()) return -1;
    return copy.get(); // returns -128..127, not 0..255
}
```
```java
// AFTER
public int read() throws IOException {
    if (!copy.hasRemaining()) return -1;
    return copy.get() & 0xFF; // unsigned: 0..255
}
```
