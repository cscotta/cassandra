## Commit
ce8c5f8497

## Ticket
KAFKA-20303

## Subsystem
Kafka Streams / State Store Internals

## Category
Logic error in condition (performance)

## Root cause
`HeadersBytesStore.convertToHeaderFormat` prepended a zero-byte (empty headers varint) to a `valueAndTimestamp` array using `ByteBuffer.allocate(1+len).put(0x00).put(array).array()`. This requires allocating a `ByteBuffer`, two `put` calls, and an `array()` copy. `System.arraycopy` into a freshly allocated array is faster because the JVM zero-initializes the first byte automatically, eliminating the `put((byte) 0x00)` call.

## Before
```java
return ByteBuffer.allocate(1 + valueAndTimestamp.length)
    .put((byte) 0x00)
    .put(valueAndTimestamp)
    .array();
```

## After
```java
final byte[] res = new byte[1 + valueAndTimestamp.length];
// res[0] == 0x00 by JVM spec
System.arraycopy(valueAndTimestamp, 0, res, 1, valueAndTimestamp.length);
return res;
```

## How to spot
Profile code that prepends a fixed header byte. `ByteBuffer`-based construction is slower than raw array manipulation for small, fixed-format payloads. JMH benchmark showed ~1.2x throughput improvement.

## Test
`HeadersBytesStoreTest` for correctness; `RawBytesExtractionBenchmark` JMH benchmark for performance regression detection.
