# Bug 0053: TrieMemoryIndex Queue Size Tracking Shared Across Columns via Thread-Local

**Commit**: dd0d8c03b9 · **Ticket**: CASSANDRA-20668
**Subsystem**: src/java/org/apache/cassandra/index/sai/memory/TrieMemoryIndex.java
**Category**: Race condition / missing synchronization

---

## Root Cause

The `Collector` class inside `TrieMemoryIndex` used a `FastThreadLocal<Integer>` to remember the last priority-queue size across range queries. Because `FastThreadLocal` is scoped to the calling thread, not to a specific index column, all SAI columns sharing a thread shared the same queue size hint. A large range query on one column would inflate the queue size hint seen by an unrelated column's next query on that same thread, wasting memory and potentially masking issues. Additionally, the `values().forEach(lambda)` pattern used to iterate trie entries allocated a lambda per call; replacing it with an explicit iterator loop eliminates that allocation. The queue size tracking was moved to an `AtomicInteger` field on the `TrieMemoryIndex` instance itself so it is correctly scoped per column.

### Before / After

```java
// BEFORE – shared FastThreadLocal across all columns on a thread
private static final FastThreadLocal<Integer> lastQueueSize = new FastThreadLocal<>() {
    protected Integer initialValue() { return MINIMUM_QUEUE_SIZE; }
};
// and using lambda iteration:
data.subtrie(...).values().forEach(cd::processContent);
```

```java
// AFTER – per-index AtomicInteger, explicit iterator
private final AtomicInteger lastPriorityQueueSize = new AtomicInteger(MINIMUM_PRIORITY_QUEUE_SIZE);
// and explicit loop:
Iterator<PrimaryKeys> values = data.subtrie(...).valueIterator();
while (values.hasNext())
    cd.processContent(values.next());
```

---

## How to Spot

A `ThreadLocal` (or `FastThreadLocal`) used as a cache hint inside a class that may have multiple instances sharing threads will incorrectly cross-contaminate state between instances. During review, ask whether the cached value is logically an instance property or a thread property; if it belongs to the instance (e.g., "the typical queue size for *this* column's range queries"), it must be an instance field, not a thread-local.

---

## Test

`TrieMemoryIndexTest` (existing range query tests) exercises range matches and implicitly validates that the queue-size hint is correctly updated per index instance.
