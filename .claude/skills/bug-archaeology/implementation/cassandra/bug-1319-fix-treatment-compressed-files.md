# Bug 1319: Fix CASSANDRA-8750's Treatment of Compressed Files

**Commit**: a1e2978f9f · **Ticket**: CASSANDRA-8750
**Subsystem**: src/java/org/apache/cassandra/io/compress/CompressionMetadata.java
**Category**: Logic error in condition

---

## Root Cause

The `CompressionMetadata.Writer#open()` method used for early-opening a compressed SSTable during streaming/compaction had two related defects. First, when opening in `SHARED` mode it asserted `dataLength % parameters.chunkLength() == 0`, but an early-open length is not guaranteed to fall on a chunk boundary — the final partially-written chunk is also valid and must be included. Second, in `FINAL` mode the code nulled out `this.offsets` before potentially sharing the reference, meaning a `SHARED_FINAL` open could expose a null reference; and the `abort()` method did not guard against a null `offsets` field, causing an NPE if `abort()` was called after a `FINAL` open. In `CompressedRandomAccessReader`, the decompressed buffer could extend past the imposed early-open length because the length cap was never applied to `validBufferBytes` after filling a chunk.

### Before / After

```java
// BEFORE: asserts chunk-boundary alignment, silently drops partial final chunk
assert dataLength % parameters.chunkLength() == 0;
count = (int) (dataLength / parameters.chunkLength());
```

```java
// AFTER: includes the partial final chunk
count = (int) (dataLength / parameters.chunkLength());
if (dataLength % parameters.chunkLength() != 0)
    count++;
```

```java
// BEFORE: abort() NPEs if offsets was nulled by a FINAL open
public void abort() { offsets.close(); }

// AFTER
public void abort() { if (offsets != null) offsets.close(); }
```

---

## How to Spot

Off-by-one errors around chunk-boundary alignment checks are common wherever variable-length chunked formats are truncated to an early length. Reviewers should ask: can the imposed length fall mid-chunk, and does the code handle that partial chunk correctly? Assertions that enforce alignment (`assert x % chunkSize == 0`) are particularly dangerous in early-open paths where partial chunks are intentional. NPE risks after reference nulling should be caught by ensuring `abort()` and other cleanup paths are null-safe.

---

## Test

`SSTableRewriterTest` substantially expanded to cover compressed early-open scenarios with non-chunk-aligned lengths and proper abort/cleanup paths.
