# Bug 0798: Partition Count Always Zero in Compaction Log Message

**Commit**: 9d90b4e2da · **Ticket**: CASSANDRA-12184
**Subsystem**: Compaction / Logging
**Category**: Logic error in condition

---

## Root Cause
The compaction completion log message included `totalSourceRows`, intended to show the total number of partitions merged across all input SSTables. However, `totalSourceRows` was declared and initialised to `0` in the caller (`runMayThrow`) and was never updated — the actual accumulation was supposed to happen inside `updateCompactionHistory()`, which only returned the merge-summary string and discarded the total. The fix introduces a `Summary` inner class holding both the partition-merge string and the computed `totalSourceRows` value, and `updateCompactionHistory()` is changed to return that object so the caller can log the correct total.

### Before / After
```java
// BEFORE
long totalSourceRows = 0;  // never updated
String mergeSummary = updateCompactionHistory(...);
logger.debug("... %,d total partitions merged to %,d ...",
             totalSourceRows,  // always 0
             totalKeysWritten, mergeSummary);

// AFTER — totalSourceRows accumulated inside updateCompactionHistory
for (int i = 0; i < mergedRowCounts.length; i++) {
    int rows = i + 1;
    totalSourceRows += rows * count;  // now computed
    ...
}
return new Summary(mergeSummary.toString(), totalSourceRows);

// caller:
Summary mergeSummary = updateCompactionHistory(...);
logger.debug("... %,d total partitions merged to %,d ...",
             mergeSummary.totalSourceRows, totalKeysWritten, mergeSummary.partitionMerge);
```

---

## How to Spot
Variables initialised to a sentinel value (0, null, empty string) before a method call that was supposed to fill them in, but where the return value or out-parameter is ignored or unused, are a classic stale-variable bug. In review, check that every variable appearing in a log format string is actually written somewhere between its declaration and its use in the log call.

---

## Test
No test added.
