# Bug 1001: EstimatedHistogram Wrong Construction in tablehistograms

**Commit**: ce7f5cc332 · **Ticket**: CASSANDRA-10859
**Subsystem**: nodetool / metrics
**Category**: Wrong constant / default value

---

## Root Cause
`nodetool tablehistograms` received raw bucket-count arrays from JMX and tried to reconstruct an `EstimatedHistogram` by first creating a throw-away histogram just to obtain its bucket offsets, then passing those offsets together with the data into the two-argument constructor.
The throw-away histogram used the wrong bucket count: `new EstimatedHistogram(estimatedPartitionSize.length)` generates offsets for `length` buckets, but the real data array has `length` entries which correspond to `length-1` buckets plus one overflow bucket — so the generated offsets did not match the data.
The fix introduced a single-argument constructor that derives the correct offsets directly from the data length, eliminating the off-by-one.

### Before / After
```java
// BEFORE — wrong offset array derived from length (off-by-one in bucket count):
long[] partitionSizeBucketOffsets = new EstimatedHistogram(estimatedPartitionSize.length).getBucketOffsets();
EstimatedHistogram partitionSizeHist = new EstimatedHistogram(partitionSizeBucketOffsets, estimatedPartitionSize);
// error message used raw offset array index:
System.err.println(String.format("Row sizes are larger than %s...", partitionSizeBucketOffsets[partitionSizeBucketOffsets.length - 1]));

// AFTER — derive offsets from data directly:
EstimatedHistogram partitionSizeHist = new EstimatedHistogram(estimatedPartitionSize);
// error message uses the histogram's own accessor:
System.err.println(String.format("Row sizes are larger than %s...", partitionSizeHist.getLargestBucketOffset()));
```

---

## How to Spot
Any code that constructs an `EstimatedHistogram` using a size-only constructor merely to extract bucket offsets and then immediately builds a second histogram from those offsets plus external data is a red flag: the two-step process introduces a bucket-count discrepancy because the `n`-entry data array corresponds to `n-1` offset buckets. Review should verify that offset arrays and data arrays are always derived from the same source.

---

## Test
No test added.
