# Bug 0934: Bloom Filter Oversized with LCS

**Commit**: a48b836b15 · **Ticket**: CASSANDRA-11344
**Subsystem**: Compaction / LCS
**Category**: Wrong constant / default value

---

## Root Cause
`MaxSSTableSizeWriter` (used by Leveled Compaction Strategy) calculated the expected number of output SSTables as `estimatedTotalKeys / maxSSTableSize`. The numerator `estimatedTotalKeys` is the de-duplicated key count after compaction, which can be significantly smaller than the total number of keys in the input SSTables. However, the denominator was the target SSTable *size in bytes*, not a key count, making the division meaningless and producing a wildly wrong estimate. Additionally, the number of estimated output SSTables was used to size the bloom filters: an overestimated count caused oversized bloom filters (wasted memory) or an underestimated count caused undersized ones (higher false-positive rate). The fix introduces `getTotalWriteSize()`, which scales the expected compacted file size by the estimated compaction ratio (de-duplicated keys / total keys), yielding a byte-count estimate that is divided by `maxSSTableSize` to correctly count output SSTables.

### Before / After
```java
// BEFORE — divides key count by byte size, nonsensical
estimatedSSTables = Math.max(1, estimatedTotalKeys / maxSSTableSize);

// AFTER — divides estimated byte output by max SSTable byte size
long totalSize = getTotalWriteSize(nonExpiredSSTables, estimatedTotalKeys, cfs, compactionType);
estimatedSSTables = Math.max(1, totalSize / maxSSTableSize);

// getTotalWriteSize applies the estimated compaction ratio
double estimatedCompactionRatio = (double) estimatedTotalKeys / estimatedKeysBeforeCompaction;
return Math.round(estimatedCompactionRatio * cfs.getExpectedCompactedFileSize(...));
```

---

## How to Spot
Any place that divides a count (keys) by a size (bytes) or vice versa to produce a meaningful estimate is a unit mismatch bug. During review, ensure that both operands of arithmetic used for resource sizing share the same unit. Also verify that bloom-filter sizing relies on correct SSTable-count estimates so that memory is not wasted and false-positive rates stay within bounds.

---

## Test
No new test added; the fix is validated by the existing LCS compaction tests.
