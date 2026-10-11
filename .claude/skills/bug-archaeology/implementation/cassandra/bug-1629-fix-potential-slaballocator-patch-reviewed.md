# Bug 1629: Fix potential SlabAllocator yield-starvation

**Commit**: d13f78ab82 · **Ticket**: CASSANDRA-7133
**Subsystem**: utils/SlabAllocator
**Category**: Race condition / missing synchronization

---

## Root Cause

`SlabAllocator` used a two-phase region-initialization scheme: the winning thread CAS-ed a bare (uninitialized) `Region` into `currentRegion`, then called `region.init()` to allocate the backing `ByteBuffer`. Loser threads detected the `UNINITIALIZED` sentinel and called `Thread.yield()` in a spin loop while waiting for `init()` to complete. Under high contention this yielding could starve the initializing thread or cause excessive context switches. The fix eliminates the deferred-init entirely: `Region` now allocates its `ByteBuffer` eagerly in its constructor, so by the time any thread sees a region it is fully ready to use. Racing losers stash their unused regions in a global `ConcurrentLinkedQueue` for reuse, avoiding wasted allocation.

### Before / After

```java
// BEFORE – spin-yield while winner initializes the region
int oldOffset = nextFreeOffset.get();
if (oldOffset == UNINITIALIZED)
{
    Thread.yield();   // potential starvation
    continue;
}
```

```java
// AFTER – region is initialized in constructor; no spin needed
private Region(int size)
{
    data = ByteBuffer.allocate(size);  // eager allocation
}
// nextFreeOffset starts at 0, never UNINITIALIZED
```

---

## How to Spot

Spin loops containing `Thread.yield()` as the only back-off are a code smell: under contention the yielding thread may never get scheduled relative to the thread it is waiting on. Prefer eager initialization so shared objects are always in a consistent state before being published, or use proper locking/futures for deferred initialization.

---

## Test

No test added.
