# Bug 2479: Compression chunk_length_kb serialized as bytes instead of kilobytes

**Commit**: 09ac4bc05b · **Ticket**: CASSANDRA-3558

## Detection Rule

When a configuration value is stored in one unit internally but serialized/emitted in a different unit, if the serialization path omits the unit conversion, readers will interpret the value with the wrong magnitude.

## Code Smell

`options.put(CHUNK_LENGTH_KB, chunkLength.toString())` where `chunkLength` is stored internally in bytes — the key says "kb" but the stored value is bytes, causing a 1024x discrepancy when the value is read back.

## Tags

- scope: cassandra
- category: wrong-serialization
- code-pattern: missing-unit-conversion-on-serialize
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause

`CompressionParameters` stored `chunkLength` internally in bytes but the `toThrift()` and `asThriftOptions()` methods serialized it raw (`chunkLength.toString()`) into a field named `chunk_length_kb`. Any reader who parsed the field would multiply by 1024 again, getting a chunk length 1024x too large. The fix adds a `chunkLengthInKB()` helper that divides by 1024 before serialization.

## Example

```java
// BEFORE
options.put(CHUNK_LENGTH_KB, chunkLength.toString()); // wrong: bytes stored as "kb"
```

```java
// AFTER
options.put(CHUNK_LENGTH_KB, chunkLengthInKB()); // correct: converts bytes → KB

private String chunkLengthInKB()
{
    return String.valueOf(chunkLength() / 1024);
}
```
