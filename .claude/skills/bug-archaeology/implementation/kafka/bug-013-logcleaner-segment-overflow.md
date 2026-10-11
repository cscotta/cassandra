**Commit**: 67d1e22070 · **Ticket**: KAFKA-20036
**Subsystem**: core — LogCleaner / Cleaner
**Category**: Logic error in condition

**Root cause**
When compression level changes between writes and compaction, compressed batches can expand beyond the original segment size budget, causing a cleaned segment to overflow its size or offset-range limit during `cleanSegments()`. The old code threw `LogCleaningAbortedException` on overflow, forcing the cleaner to abort and retry — but the same overflow would recur every time, blocking compaction permanently. The fix adds an overflow-detection path inside `cleanInto()`: when overflow is detected, the current cleaned segment is finalized and a new one started. Additionally, a per-partition ratio (starting at 1.0, multiplied by 0.9 on each overflow) reduces the segment-size budget for future cleaning attempts, preventing the overflow from recurring.

**Before/After**
```java
// Before — overflow aborts cleaning entirely
logger.info("Caught segment overflow error during cleaning: {}", e.getMessage());
throw new LogCleaningAbortedException();

// After — overflow rolls to a new cleaned segment; retry with reduced budget
if (overflowOpt.isPresent()) {
    logger.info("Completing cleaned segment {} due to overflow, creating new segment", currentCleaned.baseOffset());
    // roll to new segment, continue cleaning
}
```

**How to spot**
Any log-cleaning loop that assumes a 1:1 relationship between source segments and output segments will fail when compression expansion causes the output to exceed the size budget. Look for `cleanSegments` callers that do not handle the case where one source segment maps to multiple output segments. The retry/abort pattern without backoff or budget reduction is a red flag.

**Test**
`LogCleanerTest#testCleanedSegmentSizeOverflow` and `testCleanedSegmentOffsetOverflow` — two new unit tests construct minimal logs that trigger size and offset overflow respectively, then assert that the segment count is preserved (2 source → 2 cleaned) and all records are retained.
