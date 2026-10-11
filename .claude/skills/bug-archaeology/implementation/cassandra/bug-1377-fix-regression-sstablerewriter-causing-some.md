# Bug 1377: SSTableRewriter Regression Causes Rows to Become Unreadable During Compaction

**Commit**: 871f0039c5 · **Ticket**: CASSANDRA-8429
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableRewriter.java
**Category**: State not cleaned up

---

## Root Cause

When SSTableRewriter performed early re-opening of partially-written SSTables during compaction, it tracked pairs of `(SSTableWriter, SSTableReader)` in `finishedWriters`. The `finish()` path called `closeAndOpenReader()` on each writer to produce a real SSTable, then attempted to share the bloom filter between the old early-opened reader and the new final reader via `sharesBfWith()`. This reference-sharing mechanism was fragile: if the early reader was closed before the final reader was ready, the bloom filter could be double-freed or the shared off-heap memory could be released prematurely, making rows invisible (the bloom filter would falsely report them absent).

The fix replaces the `sharesBfWith` mechanism entirely with a `FinishType` enum (`EARLY`, `FINISH_EARLY`, `NORMAL`) that controls whether files are renamed, checksums written, and off-heap memory ownership transferred, ensuring each state transition is explicit and safe.

### Before / After

```java
// BEFORE: fragile BF sharing between early reader and final reader
if (w.right != null)
{
    w.right.sharesBfWith(newReader);
    // ...
}
toReplace.add(Pair.create(w.right, newReader));
```

```java
// AFTER: explicit finish type drives ownership; discard list cleaned atomically
SSTableReader newReader = f.writer.finish(SSTableWriter.FinishType.FINISH_EARLY, maxAge, repairedAt);
if (f.reader != null)
    f.reader.setReplacedBy(newReader);
finished.add(newReader);
```

---

## How to Spot

Any code that shares mutable off-heap resources (bloom filters, memory-mapped segments) between two objects by recording a back-pointer and conditionally skipping release is fragile. The reviewer should ask: "what happens if the object holding the pointer is closed first, or the pointer is set after the close check runs?" If the answer is "undefined behavior", the sharing model is unsafe. Prefer explicit ownership transfer through a typed state machine (like `FinishType`) over ad-hoc pointer aliasing.

---

## Test

`SSTableRewriterTest` was extended with `finishAndThrow` paths that throw exceptions at `EARLY` and after-all-finished stages to verify cleanup correctness in both the normal and abort paths.
