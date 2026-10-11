# Bug 1285: LCS Scanner Position Compared Against Compressed Byte Count Instead of Uncompressed

**Commit**: 1fab099892 · **Ticket**: CASSANDRA-9139
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTable.java
**Category**: Wrong constant / default value

---

## Root Cause

`LeveledCompactionStrategyTest` asserted that a scanner's current position (measured in uncompressed bytes read) equalled `SSTable.getTotalBytes()`, which sums `onDiskLength()` values. When SSTable compression is enabled, `onDiskLength()` returns the compressed on-disk size, which is smaller than the uncompressed size seen by the scanner. The test therefore always failed on configurations with compression. The fix introduces `SSTable.getTotalUncompressedBytes()` that sums `uncompressedLength()` instead, and updates the assertion to use it. Additionally, test data was changed from a zero-filled byte array to random bytes to defeat compression and prevent compacted files from collapsing.

### Before / After

```java
// BEFORE — getTotalBytes sums compressed on-disk sizes; wrong for compressed SSTables
assert scanner.getCurrentPosition() == SSTable.getTotalBytes(sstables);
```

```java
// AFTER — new helper sums uncompressed lengths, matching the scanner's progress metric
assertEquals(scanner.getCurrentPosition(), SSTable.getTotalUncompressedBytes(sstables));
```

---

## How to Spot

Any assertion or calculation that mixes a "bytes scanned" progress counter with a "bytes on disk" size total is suspect when compression is involved. Always ask: is this size in compressed or uncompressed units? Introduce explicit method names (`getTotalUncompressedBytes` vs `getTotalBytes`) rather than relying on callers to remember which unit `getTotalBytes` uses. Also watch for test data that is all-zeros or all-same-value: compressors collapse such data aggressively, masking size mismatches.

---

## Test

`LeveledCompactionStrategyTest` updated: assertion corrected to use `getTotalUncompressedBytes`, and test data changed to random bytes to ensure compression does not trivially eliminate size differences.
