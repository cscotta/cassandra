# Bug 1007: CqlInputFormat Split Size Regression

**Commit**: 4f3e47bf9e · **Ticket**: CASSANDRA-10835
**Subsystem**: Hadoop integration / CqlInputFormat
**Category**: Logic error in condition

---

## Root Cause
`AbstractColumnFamilyInputFormat.describeSplits` computed the split count as `(meanPartitionSize * partitionCount) / splitSize`, where `splitSize` was the value of `cassandra.input.split.size` (number of partitions per split, defaulting to 65536).
Dividing byte-units (the product of mean partition bytes and partition count) by a partition-count unit produces a dimensionally incorrect result — a very large number of tiny splits for non-trivial data.
A separate MB-based split size setting (`cassandra.input.split.size_mb`) existed but was never read by the split-computation path.
The fix reads both settings and selects the MB-based formula when `splitSizeMb > 0`, otherwise falls back to the partition-count formula: `partitionCount / splitSize`.

### Before / After
```java
// BEFORE — bytes divided by partition count (wrong units):
int splitCount = (int)((meanPartitionSize * partitionCount) / splitSize);

// AFTER — branch on which setting is active:
int splitSizeMb = ConfigHelper.getInputSplitSizeInMb(conf);
...
int splitCount = splitSizeMb > 0
    ? (int)(meanPartitionSize * partitionCount / splitSizeMb / 1024 / 1024)
    : (int)(partitionCount / splitSize);
```

---

## How to Spot
Dimensional analysis: whenever a split-count formula multiplies `meanPartitionSize` (bytes) by `partitionCount` and divides by a constant that was documented as a partition count, the units don't cancel. Review the javadoc for `setInputSplitSize` — it says "number of partitions" — so dividing byte-sized quantities by it is wrong. When two configuration keys control the same logical parameter, both code paths should be exercised in tests.

---

## Test
No test added.
