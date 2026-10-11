# Bug 1029: CompressionInfo File Not Fsynced on Close

**Commit**: 7e056fa270 · **Ticket**: CASSANDRA-10534
**Subsystem**: I/O / compression metadata
**Category**: Non-atomic file write

---

## Root Cause
`CompressionMetadata.Writer.close()` wrote the compression offset table through a `BufferedOutputStream` wrapping a `FileOutputStream`, but never called `flush()` on the buffered stream or `sync()` on the underlying file descriptor before closing. This meant that on a JVM crash or OS panic immediately after `close()`, buffered data still in the OS page cache could be lost, leaving a partial or empty `CompressionInfo` file on disk. The fix also split `fos` (the raw `FileOutputStream`) out as a separate local variable so that `getFD().sync()` could be called after flushing the buffer.

### Before / After
```java
// BEFORE — buffer never flushed, fd never synced
out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)));
assert chunks == count;
writeHeader(out, dataLength, chunks);
for (int i = 0; i < count; i++)
    out.writeLong(offsets.getLong(i * 8L));
// falls through to finally { FileUtils.closeQuietly(out); }

// AFTER — explicit flush + fsync before close
fos = new FileOutputStream(filePath);
out = new DataOutputStream(new BufferedOutputStream(fos));
assert chunks == count;
writeHeader(out, dataLength, chunks);
for (int i = 0; i < count; i++)
    out.writeLong(offsets.getLong(i * 8L));

out.flush();
fos.getFD().sync();
// then finally { FileUtils.closeQuietly(out); }
```

---

## How to Spot
Every code path that writes durability-critical metadata (compression info, index files, manifest files) must call `flush()` then `fd.sync()` (or use a utility like `CLibrary.trySync`) before closing. A `BufferedOutputStream` wrapping a `FileOutputStream` without an explicit `flush()` before close silently loses data in the buffer. Review any `new DataOutputStream(new BufferedOutputStream(new FileOutputStream(...)))` usage to confirm both flush and sync are present.

---

## Test
No test added.
