# Bug 1215: Memory Leak in Ref.GlobalState Due to ConcurrentLinkedQueue.remove() Behaviour

**Commit**: 7c5fc40b8b · **Ticket**: CASSANDRA-9549
**Subsystem**: src/java/org/apache/cassandra/utils/concurrent/Ref.java
**Category**: Resource leak

---

## Root Cause

`Ref.GlobalState` held a `ConcurrentLinkedQueue<State>` called `locallyExtant` to track all live `PhantomReference` instances for a reference-counted object. Each time a `Ref` was created, its `State` was added to this queue; each time it was released, the corresponding `State` was removed. `ConcurrentLinkedQueue.remove(Object)` scans the entire queue to find and unlink the target node, but the unlinked node retains a pointer to the next node until a subsequent traversal GC-cleans internal links — a well-known characteristic of the implementation. Under workloads that repeatedly create and release refs, removed nodes accumulated as phantom-reachable garbage not promptly collected, causing the queue's memory footprint to grow without bound. Replacing the queue with `ConcurrentLinkedDeque` uses an implementation that cleans internal links more eagerly on removal.

### Before / After

```java
// BEFORE
private final ConcurrentLinkedQueue<State> locallyExtant = new ConcurrentLinkedQueue<>();
```

```java
// AFTER
private final Collection<State> locallyExtant = new ConcurrentLinkedDeque<>();
```

---

## How to Spot

`ConcurrentLinkedQueue.remove(Object)` is documented to be O(n) and leaves internal node references in place until a future traversal; using it as a membership set for objects that are frequently added and removed is a common source of memory leaks. Code that populates a `ConcurrentLinkedQueue` with `add()` and then cleans it with `remove(Object)` in a hot path should be flagged during review.

---

## Test

`RefCountedTest.testMemoryLeak` creates a `Ref`, takes 1 000 child refs and immediately releases each one, then measures the heap size of the root `Ref` with `ObjectSizes.measureDeep` and asserts it has not grown to more than twice its initial size.
