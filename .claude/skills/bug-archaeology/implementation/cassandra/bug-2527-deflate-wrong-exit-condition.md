# Bug 2527: DeflateCompressor loop exits on needsInput() instead of finished()

**Commit**: 6f38e10b10 · **Ticket**: CASSANDRA-3370

## Detection Rule
When driving a `java.util.zip.Deflater` in a loop, use `deflater.finished()` as the exit condition, not `deflater.needsInput()`; for incompressible data the output can be larger than the input so `needsInput()` becomes `true` while the deflater still has buffered output to flush.

## Code Smell
`if (def.needsInput()) { return offs - outputOffset; }` inside a deflate loop — `needsInput()` returns `true` as soon as the deflater has consumed all input, but it may still have pending output bytes; returning at that point silently truncates the compressed output.

## Tags
- scope: java
- category: wrong-filter-result
- code-pattern: wrong-deflater-termination-check
- severity: data-loss
- subsystem: storage-engine

## Root Cause
The `DeflateCompressor.compress()` loop feeds input to the JDK `Deflater` and collects output in chunks. For data that does not compress well (expansion case), the deflater needs multiple write iterations after all input has been consumed to flush the remaining compressed bytes. `needsInput()` returns `true` after input is exhausted, so the loop returned prematurely, writing a truncated (and therefore undecompressible) compressed block to disk. The correct exit condition is `finished()`, which becomes `true` only after all output has been flushed.

## Example
```java
// BEFORE
while (true) {
    offs += def.deflate(output.buffer, offs, output.buffer.length - offs);
    if (def.needsInput())          // exits too early for incompressible data
        return offs - outputOffset;
}
```
```java
// AFTER
while (true) {
    offs += def.deflate(output.buffer, offs, output.buffer.length - offs);
    if (def.finished())            // exits only after all output flushed
        return offs - outputOffset;
}
```
