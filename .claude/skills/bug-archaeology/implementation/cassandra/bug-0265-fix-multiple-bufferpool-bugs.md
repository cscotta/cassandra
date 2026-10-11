# Bug 0265: BufferPool Race Conditions in Chunk Recycling and Wrong Metrics Accounting

**Commit**: e13356d75d · **Ticket**: CASSANDRA-16681
**Subsystem**: src/java/org/apache/cassandra/utils/memory/BufferPool.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`BufferPool` had several interacting bugs. First, `MicroQueueOfChunks.clearForEach` saved `count` after zeroing it, so the subsequent `forEach` always saw `count == 0` and never processed any chunks. Second, the `free(buffer, tryRelease)` method attempted to atomically transition `freeSlots` from all-ones (`-1L`) to zero to claim recycling responsibility, but this was racy: when a chunk was freed from a thread that did not own the `LocalPool` (e.g. a tiny-chunk slab returned by a different thread), the recycling could happen on the wrong thread and corrupt the non-thread-safe local queue. Third, the `LocalPool.release()` method released tiny pool chunks after clearing the parent pool's reference, so tiny pool recycling could access already-freed state. Finally, `hits`/`misses` metrics were recorded before the pool lookup, meaning a miss counted as a hit if allocation from the heap eventually succeeded.

### Before / After

```java
// BEFORE – clearForEach: saves count=0 after zeroing, forEach does nothing
this.chunk0 = this.chunk1 = this.chunk2 = null;
forEach(consumer, count, chunk0, chunk1, chunk2);
count = 0;
```

```java
// AFTER – saves old count first, then clears
int oldCount = count;
count = 0;
this.chunk0 = this.chunk1 = this.chunk2 = null;
forEach(consumer, oldCount, chunk0, chunk1, chunk2);
```

---

## How to Spot

Multi-step "save-then-clear" patterns that zero the field before capturing it are a classic ordering bug. In recycling logic, pay special attention to which thread is performing the recycle and whether the data structure being modified (here, the local chunk queue) is thread-safe—if not, any cross-thread recycling path must be guarded. In `LocalPool.release`, child resources (tinyPool) should be released before the parent to avoid use-after-free ordering.

---

## Test

`BufferPoolTest.testTryGet` verifies that allocating and then returning a buffer via `put` leaves `usedSizeInBytes` at zero and returns the chunk to the global pool.
