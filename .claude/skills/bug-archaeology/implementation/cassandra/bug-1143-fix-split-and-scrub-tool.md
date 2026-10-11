# Bug 1143: Split and Scrub Tools Leave SSTable Files Undeleted

**Commit**: 605bcdcf11 · **Ticket**: CASSANDRA-7066
**Subsystem**: src/java/org/apache/cassandra/db/compaction/writers/CompactionAwareWriter.java
**Category**: State not cleaned up

---

## Root Cause

The `CompactionAwareWriter` hierarchy passed the `offline` flag to `SSTableRewriter.keepOriginals()`, meaning that whenever a compaction ran offline (e.g. via the split or scrub tools) the original SSTables were unconditionally preserved on disk. This was wrong for scrub and split, which do intend to replace or remove the originals. Additionally, `SSTableTxnWriter` committed the writer before the transaction (so on abort the transaction cleanup ran against already-committed writer state), and `StandaloneSplitter` manually called `transaction.obsoleteOriginals()` followed by `sstable.selfRef().release()` — both operations that are now handled inside the transaction lifecycle. The combination left orphan files on disk after split or scrub completed.

### Before / After

```java
// BEFORE (CompactionAwareWriter)
this.sstableWriter = new SSTableRewriter(cfs, txn, maxAge, offline).keepOriginals(offline);

// BEFORE (SSTableTxnWriter)
protected Throwable doCommit(Throwable accumulate) {
    return txn.commit(writer.commit(accumulate));
}
protected void doPrepare() {
    writer.prepareToCommit();
    txn.prepareToCommit();
}
```

```java
// AFTER (CompactionAwareWriter)
this.sstableWriter = new SSTableRewriter(cfs, txn, maxAge, offline).keepOriginals(keepOriginals);

// AFTER (SSTableTxnWriter)
protected Throwable doCommit(Throwable accumulate) {
    return writer.commit(txn.commit(accumulate));
}
protected void doPrepare() {
    txn.prepareToCommit();
    writer.prepareToCommit();
}
```

---

## How to Spot

When an `offline` flag is repurposed as a proxy for `keepOriginals`, any caller that is offline but does *not* want to keep originals will misbehave. Boolean parameters that conflate two orthogonal concerns (execution context vs. desired side-effect) should be split. Also review transactional commit/prepare orderings: the writer must commit after the transaction to ensure the transaction log records the outcome before the writer's files are finalized; reversing the order risks leaving files in an ambiguous state on crash.

---

## Test

`ScrubTest` and `SSTableRewriterTest` were updated to use `TransactionLogs.waitForDeletions()` and to remove manual `selfRef().release()` calls that are now handled by the transaction. `CompactionAwareWriterTest` was updated to use the new no-`offline` constructors.
