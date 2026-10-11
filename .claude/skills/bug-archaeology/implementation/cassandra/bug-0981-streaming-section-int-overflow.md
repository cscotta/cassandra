# Bug 0981: Streaming Sections Larger Than 2GB Overflow int

**Commit**: 582bdba4b2 · **Ticket**: CASSANDRA-10961
**Subsystem**: Streaming
**Category**: Off-by-one

---

## Root Cause
`CompressedStreamReader.read()` computed the length of each compressed section as `int sectionLength = (int)(section.right - section.left)`. Because section boundaries are `Long` values and a single compressed section can exceed `Integer.MAX_VALUE` (2 GB), the cast silently truncates the value, producing a negative or wildly wrong section length. The reader then terminates the inner loop far too early (or loops indefinitely), corrupting or truncating the streamed SSTable.

The same pattern in the uncompressed path was also affected. The fix changes `sectionLength` to `long` throughout, so the loop condition `in.getBytesRead() < sectionLength` remains correct for sections of any size.

### Before / After
```java
// BEFORE – int overflow for sections > 2 GB
int sectionLength = (int) (section.right - section.left);
while (in.getBytesRead() < sectionLength) { ... }

// AFTER – long comparison, no overflow
long sectionLength = section.right - section.left;
while (in.getBytesRead() < sectionLength) { ... }
```

---

## How to Spot
Any arithmetic or cast involving file offsets, byte counts, or sizes that uses `int` where the value could exceed 2 GB is a latent overflow bug. When reviewing streaming, compaction, or I/O code, trace all variables that represent byte positions or lengths and confirm they are `long` throughout. A narrowing cast `(int)(long expression)` on a size variable is almost always wrong.

---

## Test
`CompressedInputStreamTest` is removed (it was testing internal behaviour that changed), and the fix is validated through the broader streaming integration paths. No dedicated unit test for the overflow case is added.
