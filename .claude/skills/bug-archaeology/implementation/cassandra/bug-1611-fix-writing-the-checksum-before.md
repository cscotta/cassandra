# Bug 1611: Checksum Written Before Closing the SSTable

**Commit**: 9bd3887805 · **Ticket**: CASSANDRA-7285
**Subsystem**: io/sstable/SSTableWriter
**Category**: Non-atomic file write

---

## Root Cause

In `SSTableWriter.close()`, the full checksum was written to the data file *before* `dataFile.close()` was called. The problem is that `dataFile.close()` can truncate the file if there is buffered data that did not get written, or if the file was opened with a size that differs from the actual written bytes. Writing the checksum before the final close means the checksum is computed over a file whose content may still change (truncation), resulting in a checksum that does not match the final on-disk data.

The fix moves `dataFile.writeFullChecksum(descriptor)` to after `dataFile.close()`, ensuring the checksum is computed only once the file has reached its final, stable state.

### Before / After

```java
// BEFORE — checksum computed before close/truncation
dataFile.writeFullChecksum(descriptor);  // over potentially-unstable bytes
iwriter.close();
dataFile.close();  // may truncate
```

```java
// AFTER — checksum computed after the file is finalized
iwriter.close();
dataFile.close();  // file is now stable
dataFile.writeFullChecksum(descriptor);  // checksum matches final content
```

---

## How to Spot

When writing a file that includes a checksum or digest, the checksum step must always be the last operation on the file content. Any sequence where a checksum/sync/flush precedes a `close()` or `truncate()` on the same file should be reviewed.

---

## Test

No test added in this commit. The scenario is: write an SSTable, close it, then open and validate it — the CRC/checksum verification should pass on the freshly written file.
