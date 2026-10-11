# Bug 1278: Integer Overflow When Calculating Max SSTable Size in LCS

**Commit**: 35a4f7dd68 · **Ticket**: CASSANDRA-9235
**Subsystem**: db/compaction/LeveledManifest.java
**Category**: Off-by-one

---

## Root Cause

`LeveledManifest` stored `maxSSTableSizeInBytes` as an `int` and computed it by multiplying an `int` megabyte value by `1024 * 1024`. Both operands are `int`, so the multiplication is performed in 32-bit arithmetic and overflows for any `maxSSTableSizeInMB` value above 2047 MB. The overflowed (negative or wrong) byte threshold causes LCS to make incorrect compaction decisions—either never compacting large SSTables or compacting them far too aggressively. The fix is to promote the field to `long` and force the literal constants to `long` arithmetic with `1024L`.

### Before / After

```java
// BEFORE
private final int maxSSTableSizeInBytes;
...
this.maxSSTableSizeInBytes = maxSSTableSizeInMB * 1024 * 1024;
```

```java
// AFTER
private final long maxSSTableSizeInBytes;
...
this.maxSSTableSizeInBytes = maxSSTableSizeInMB * 1024L * 1024L;
```

---

## How to Spot

Any time a size value is stored after converting from a user-facing unit (MB, GB) via integer multiplication, check whether both operands are `int`. The product of two `int` values silently overflows at 2 GB, which is a common threshold for storage systems that support configurable sstable sizes. The fix pattern is always the same: declare the target field as `long` and append `L` to one literal in the multiplication chain to force widening before overflow can occur.

---

## Test

No test added.
