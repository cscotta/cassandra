# Bug 0535: Resource Leak in CompressedSequentialWriter Buffer Reallocation

**Commit**: cbecd5f283 · **Ticket**: CASSANDRA-15340
**Subsystem**: `src/java/org/apache/cassandra/io/compress/CompressedSequentialWriter.java`
**Category**: Resource leak

---

## Root Cause

During truncation/reset of a `CompressedSequentialWriter`, the code checked whether the existing `compressed` buffer had sufficient capacity for the current chunk and, if not, allocated a new buffer. However, the old direct buffer was simply abandoned — `FileUtils.clean()` was never called on it before the reference was overwritten. Since these are direct (off-heap) ByteBuffers, they are not reclaimed by ordinary GC and must be explicitly cleaned, so each reallocation leaked native memory until the JVM eventually reclaimed it via a finalization path.

### Before / After

```java
// BEFORE: old buffer silently abandoned
if (compressed.capacity() < chunkSize)
    compressed = compressor.preferredBufferType().allocate(chunkSize);
```

```java
// AFTER: old buffer explicitly cleaned before replacement
if (compressed.capacity() < chunkSize)
{
    FileUtils.clean(compressed);
    compressed = compressor.preferredBufferType().allocate(chunkSize);
}
```

---

## How to Spot

Whenever a direct `ByteBuffer` field is reassigned inside a conditional block, check whether `FileUtils.clean()` (or the equivalent `sun.misc.Cleaner` invocation) is called on the old buffer first. The pattern `if (buf.capacity() < needed) buf = allocate(needed)` without a preceding `clean(buf)` is a reliable indicator of a direct buffer leak.

---

## Test

No test added.
