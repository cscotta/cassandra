# Bug 0214: Memory Read/Write Corruption on Big-Endian Unaligned Architectures (s390x)

**Commit**: 5815f0d5eb · **Ticket**: CASSANDRA-17723
**Subsystem**: src/java/org/apache/cassandra/io/util/Memory.java · src/java/org/apache/cassandra/utils/memory/MemoryUtil.java · src/java/org/apache/cassandra/io/sstable/indexsummary/IndexSummaryBuilder.java · src/java/org/apache/cassandra/utils/Architecture.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

The fast-path `unsafe.getInt/putInt/getLong/putLong` calls in `Memory` and
`MemoryUtil` assumed that if the architecture supports unaligned access the
native byte order matches the Cassandra on-disk (little-endian) format.  On
big-endian unaligned architectures such as s390x the unaligned `unsafe` reads and
writes silently produced byte-swapped values, corrupting every multi-byte field.
Additionally, `IndexSummaryBuilder` initialised its `SafeMemoryWriter` buffers
with `ByteOrder.nativeOrder()`, which on big-endian platforms stored offsets in
big-endian order, making the serialised index summary file unreadable on any other
machine.  The fix adds `Architecture.BIG_ENDIAN` and applies `reverseBytes()` in
the unaligned fast-paths throughout, and hard-codes `ByteOrder.LITTLE_ENDIAN` for
the `SafeMemoryWriter` buffers used for the on-disk format.

### Before / After

```java
// BEFORE – Memory.java, unaligned fast-path (no byte-swap for big-endian)
if (Architecture.IS_UNALIGNED)
    unsafe.putLong(peer + offset, l);
```

```java
// AFTER – byte-swap when native order is big-endian
if (Architecture.IS_UNALIGNED)
    unsafe.putLong(peer + offset, Architecture.BIG_ENDIAN ? Long.reverseBytes(l) : l);
```

```java
// BEFORE – IndexSummaryBuilder uses native byte order for on-disk buffers
offsets = new SafeMemoryWriter(4 * maxExpectedEntries).order(ByteOrder.nativeOrder());
```

```java
// AFTER – always little-endian to match the on-disk format
offsets = new SafeMemoryWriter(4 * maxExpectedEntries).order(ByteOrder.LITTLE_ENDIAN);
```

---

## How to Spot

Any `unsafe.putX/getX` call in an `IS_UNALIGNED` branch that does not account for
`BIG_ENDIAN` is potentially writing/reading incorrect values on big-endian
hardware.  Similarly, any `SafeMemoryWriter`/`ByteBuffer` whose byte order is set
to `nativeOrder()` for data that must be portable (on-disk or over the wire)
should instead use an explicit endianness.

---

## Test

No test added (architecture-specific; s390x is unsupported).
