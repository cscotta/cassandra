# Bug 0849: Compaction Throttle Not Applied to Actual Bytes Read

**Commit**: 10649b1ce5 · **Ticket**: CASSANDRA-12366
**Subsystem**: Compaction
**Category**: Logic error in condition

---

## Root Cause
The compaction rate limiter was passed into `SSTableScanner` at scanner-creation time, causing it to throttle against the raw compressed bytes as they were read off disk rather than against the logical (uncompressed) bytes being processed.  For compressed tables this produced wrong throttle accounting; for the cleanup and repair compaction paths, the limiter was not applied at all because it was threaded through layers that never actually called `acquire()`.  The fix removes the limiter from the scanner entirely and instead calls `limiter.acquire()` explicitly in the compaction loop, scaled by the compression ratio, so the rate limit correctly represents the on-disk bytes consumed.

### Before / After
```java
// BEFORE – limiter passed into scanner at creation; throttling was implicit and compression-unaware
RateLimiter limiter = CompactionManager.instance.getRateLimiter();
scanners.add(sstable.getScanner(ranges, limiter));

// AFTER – scanner gets null; caller acquires explicitly with compression ratio applied
scanners.add(sstable.getScanner(ranges, null));
// ... in the compaction loop:
long bytesScanned = scanners.getTotalBytesScanned();
int lengthRead = (int)(Ints.checkedCast(bytesScanned - lastBytesScanned) * compressionRatio);
limiter.acquire(lengthRead + 1);
lastBytesScanned = bytesScanned;
```

---

## How to Spot
When a rate limiter is forwarded deep into a helper class, verify that `acquire()` is actually called and that the argument correctly reflects the resource being limited (compressed vs. uncompressed bytes, number of rows vs. bytes, etc.).  Rate-limiting code is especially fragile across refactors because the limiter can be passed around without anyone noticing it is never used.

---

## Test
`CompactionIteratorTest` extended with a check that confirms bytes-scanned accounting works correctly across compressed and uncompressed tables.
