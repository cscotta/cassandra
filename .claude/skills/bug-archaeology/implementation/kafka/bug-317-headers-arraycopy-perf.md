## Commit
a83dda82eb

## Ticket
KAFKA-20249

## Subsystem
Kafka Streams / State Store Internals

## Category
Logic error in condition (performance)

## Root cause
Headers-aware deserializers in `AbstractRocksDBTimeOrderedSegmentedBytesStore` unnecessarily deserialized and copied the entire headers byte array when only skipping over it. Using `System.arraycopy` instead of `ByteBuffer.put` in `convertToHeaderFormat` and similar paths achieves a 2-6x speedup for non-empty headers and 1.2-1.3x for empty headers.

## Before
```java
return ByteBuffer
    .allocate(1 + valueAndTimestamp.length)
    .put((byte) 0x00)
    .put(valueAndTimestamp)
    .array();
```

## After
```java
final byte[] res = new byte[1 + valueAndTimestamp.length];
// res[0] is 0x00 by JVM default
System.arraycopy(valueAndTimestamp, 0, res, 1, valueAndTimestamp.length);
return res;
```

## How to spot
Profile `HeadersBytesStore.convertToHeaderFormat` under load. `ByteBuffer.allocate` + `put` generates more garbage than a single array allocation + `System.arraycopy`. JMH benchmarks showed ~1.2x improvement even for the empty-headers case.

## Test
`RawBytesExtractionBenchmark` JMH benchmark; `HeadersBytesStoreTest` for correctness.
