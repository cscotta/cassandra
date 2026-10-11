# Bug 1672: Fix sstableloader NPE

**Commit**: 6658a6e03c · **Ticket**: CASSANDRA-7015
**Subsystem**: io/sstable/SSTableReader
**Category**: Missing null / bounds check

---

## Root Cause

When the sstableloader opens an SSTable outside the normal database lifecycle it uses a special code path that must avoid the file-descriptor pool (which may not be initialized). The code was calling `SegmentedFile.getBuilder()` and `SegmentedFile.getCompressedBuilder()`, which are factory methods that return pooled builders. Those factory methods assumed the pool was available and returned `null` (or threw an NPE) when invoked from the sstableloader context. The fix bypasses the factory entirely and directly instantiates `BufferedSegmentedFile.Builder` and `CompressedSegmentedFile.Builder`, which have no dependency on the pool.

### Before / After

```java
// BEFORE
SegmentedFile.Builder ibuilder = SegmentedFile.getBuilder(DatabaseDescriptor.getIndexAccessMode());
SegmentedFile.Builder dbuilder = sstable.compression
                               ? SegmentedFile.getCompressedBuilder()
                               : SegmentedFile.getBuilder(DatabaseDescriptor.getDiskAccessMode());
```

```java
// AFTER
SegmentedFile.Builder ibuilder = new BufferedSegmentedFile.Builder();
SegmentedFile.Builder dbuilder = sstable.compression
                               ? new CompressedSegmentedFile.Builder()
                               : new BufferedSegmentedFile.Builder();
```

---

## How to Spot

When a class has both a factory method and a public constructor, prefer the constructor in contexts where the factory's supporting infrastructure (pool, registry, etc.) may not be initialised. Look for factory-method calls inside static or standalone-tool code paths that execute without a full server lifecycle.

---

## Test

No test added.
