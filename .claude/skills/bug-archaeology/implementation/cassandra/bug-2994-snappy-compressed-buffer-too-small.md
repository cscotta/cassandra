# Bug 2994: Snappy compressed buffer too small causing ArrayOutOfBoundsException

**Commit**: 1e36fb1e44 · **Ticket**: CASSANDRA-2994

## Detection Rule
When allocating a buffer to hold Snappy-compressed output, use `Snappy.maxCompressedLength(inputLength)` rather than `inputLength` because compressed output can exceed the input size for small or incompressible data.

## Code Smell
`compressed = new byte[metadata.chunkLength]` or `new byte[buffer.length]` used as the Snappy output buffer will be too small for chunks where compression produces larger output than the input, causing an `ArrayIndexOutOfBoundsException`.

## Tags
- scope: cassandra
- category: off-by-one
- code-pattern: compression-output-buffer-undersized
- severity: crash
- subsystem: storage-engine

## Root Cause
Both `CompressedRandomAccessReader` and `CompressedSequentialWriter` allocated their compressed-data buffers at exactly `chunkLength` / `buffer.length` bytes. Snappy (and other compressors) can produce output larger than the input for incompressible data, exceeding the allocated array and throwing `ArrayIndexOutOfBoundsException`. The fix uses `Snappy.maxCompressedLength()` to compute the correct upper bound.

## Example
```java
// BEFORE
compressed = new byte[metadata.chunkLength];       // reader
compressed = new byte[buffer.length];              // writer
```
```java
// AFTER
compressed = new byte[Snappy.maxCompressedLength(metadata.chunkLength)];  // reader
compressed = new byte[Snappy.maxCompressedLength(buffer.length)];         // writer
```
