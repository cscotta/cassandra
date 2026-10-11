# Bug 0241: BufferPool.memoryInUse Counter Incorrectly Decremented

**Commit**: 3cfb222122 · **Ticket**: CASSANDRA-18311
**Subsystem**: utils/memory/BufferPool
**Category**: Logic error in condition

---

## Root Cause

`BufferPool.LocalPool.putUnusedPortion()` computed the size to subtract from `memoryInUse` as `buffer.capacity() - buffer.limit()` (the unused tail of the provided buffer). However, `chunk.freeUnusedPortion(buffer)` may round the freed region down to a pool allocation unit, so the buffer's capacity may shrink by a different amount than the unused portion. The fix captures the original capacity before the call and computes the actual freed bytes as `buffer.capacity() - originalCapacity` (a negative delta applied to the counter), which correctly accounts for what the pool actually reclaimed rather than what the caller passed in.

### Before / After

```java
// BEFORE
int size = buffer.capacity() - buffer.limit();
chunk.freeUnusedPortion(buffer);
memoryInUse.add(-size);
```

```java
// AFTER
int originalCapacity = buffer.capacity();
int size = originalCapacity - buffer.limit();
chunk.freeUnusedPortion(buffer);
memoryInUse.add(buffer.capacity() - originalCapacity); // negative when capacity shrank
```

---

## How to Spot

When a "free" operation on a pool involves rounding or partial returns, accounting the freed amount before the operation completes will give the wrong value. The pattern to watch for is computing a delta from one field of a mutable object, then passing the object to a mutating call, and using the pre-mutation delta in a post-mutation accounting update. The delta must be captured around the mutation, not inferred from values before it.

---

## Test

A new `testPuttingUnusedPortion()` unit test in `BufferPoolTest` exercises the case where the unused portion is below the tiny-allocation-unit boundary and verifies that `usedSizeInBytes()` is unchanged, then tests an allocation-boundary-aligned limit and asserts the counter drops by exactly one allocation unit.
