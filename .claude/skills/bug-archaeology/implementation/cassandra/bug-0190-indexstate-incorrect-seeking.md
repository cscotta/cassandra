# Bug 0190: IndexState Incorrect Seeking Corrupts SSTable Iterator Deserialization

**Commit**: 7a2bfdc56d · **Ticket**: CASSANDRA-18932
**Subsystem**: io/sstable/format/big
**Category**: Logic error in condition

---

## Root Cause

`IndexState.setToBlock` had two problems when repositioning the SSTable row iterator. First, when seeking forward to a block, it called `reader.seekToPosition(columnOffset(blockIdx))` — which internally clears the deserializer state — and then called `reader.deserializer.clearState()` a second time after setting the mark, which was redundant and could discard partially-consumed state. Second, when updating the mark within the current block, it called `reader.seekToPosition(startOfBlock)` and `reader.seekToPosition(currentFilePointer)` instead of the lower-level `reader.file.seek(...)`. `seekToPosition` calls `deserializer.clearState()` as a side effect, so the deserializer's internal buffer was wiped even though the underlying file position was being set to an arbitrary mid-block offset, causing subsequent reads to start from the wrong logical position.

### Before / After

```java
// BEFORE (seek into block)
reader.seekToPosition(columnOffset(blockIdx));
mark = reader.file.mark();
reader.deserializer.clearState();  // double-clear, was already done by seekToPosition
```

```java
// AFTER (seek into block)
reader.seekToPosition(columnOffset(blockIdx));  // clears state once, correctly
mark = reader.file.mark();
// redundant clearState() removed
```

```java
// BEFORE (update mark in current block)
reader.seekToPosition(startOfBlock);
mark = reader.file.mark();
reader.seekToPosition(currentFilePointer);
```

```java
// AFTER (update mark in current block)
reader.file.seek(startOfBlock);   // raw seek, no deserializer side-effect
mark = reader.file.mark();
reader.file.seek(currentFilePointer);
```

---

## How to Spot

When a method wraps a low-level seek and has documented side effects (like clearing a deserializer state machine), callers must choose between the high-level wrapper and the raw seek based on whether those side effects are desired. Mixed use of the two in the same logical operation is a red flag; code review should ask "does every call to seekToPosition here intentionally reset the deserializer?"

---

## Test

`ColumnIndexTest` exercises iteration over SSTables with column indexes, verifying that rows are read correctly even when the iterator seeks across index block boundaries.
