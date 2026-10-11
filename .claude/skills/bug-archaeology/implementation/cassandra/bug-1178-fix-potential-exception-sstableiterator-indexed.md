# Bug 1178: Fix Potential Exception in SSTableIterator Indexed Reader

**Commit**: 9ddce25ff7 · **Ticket**: CASSANDRA-9859
**Subsystem**: src/java/org/apache/cassandra/db/columniterator/SSTableIterator.java
**Category**: Missing null / bounds check

---

## Root Cause

`SSTableIterator`'s `computeNext` method called `indexState.setToBlock(currentBlockIdx + 1)` whenever `isPastCurrentBlock()` returned true, without checking whether `currentBlockIdx + 1` was a valid index. Meanwhile, `isPastCurrentBlock()` itself included a stale guard (`currentIndexIdx < indexes.size()`) that prevented the out-of-bounds call in practice under the old code path but masked the real problem. The fix removes the redundant guard from `isPastCurrentBlock()` and instead adds the bounds check (`currentBlockIdx + 1 < indexState.blocksCount()`) at the call site where the advance actually happens, making the intent explicit and preventing an `ArrayIndexOutOfBoundsException` when the reader is already on the last index block.

### Before / After

```java
// BEFORE
public boolean isPastCurrentBlock() {
    return currentIndexIdx < indexes.size() && reader.file.bytesPastMark(mark) >= currentIndex().width;
}
// ...
if (indexState.isPastCurrentBlock())
    indexState.setToBlock(indexState.currentBlockIdx() + 1);
```

```java
// AFTER
public boolean isPastCurrentBlock() {
    return reader.file.bytesPastMark(mark) >= currentIndex().width;
}
// ...
int currentBlockIdx = indexState.currentBlockIdx();
if (indexState.isPastCurrentBlock() && currentBlockIdx + 1 < indexState.blocksCount())
    indexState.setToBlock(currentBlockIdx + 1);
```

---

## How to Spot

Any time an increment (`idx + 1`) is passed to an indexed accessor without a preceding bounds check, there is a potential for an out-of-bounds exception at the boundary. Reviewers should verify that every `setToBlock` / `setToIndex` call is guarded by a `< size` check, not merely by a predicate that happens to exclude the last element as a side-effect.

---

## Test

No dedicated test added; the fix was validated by the existing integration suite exercising indexed SSTable reads near block boundaries.
