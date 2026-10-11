# Bug 0573: iter.next() Called Multiple Times Per Iteration Notifying Secondary Indexers

**Commit**: 30d2835809 · **Ticket**: CASSANDRA-14794
**Subsystem**: `src/java/org/apache/cassandra/index/SecondaryIndexManager.java`
**Category**: Logic error in condition

---

## Root Cause

When notifying multiple secondary indexers about range tombstones, the code called `iter.next()` inside a `forEach` lambda. Because the lambda captured the iterator reference rather than the current element, each indexer in the list triggered a separate call to `iter.next()`, advancing the iterator once per indexer rather than once per tombstone. The result was that each indexer received a different range tombstone, and most tombstones were silently skipped for all but the first indexer. The fix materializes each tombstone into a local variable before the `forEach` call so all indexers receive the same instance.

### Before / After

```java
// BEFORE
Iterator<RangeTombstone> iter = deletionInfo.rangeIterator(false);
while (iter.hasNext())
    indexers.forEach(indexer -> indexer.rangeTombstone(iter.next()));
```

```java
// AFTER
Iterator<RangeTombstone> iter = deletionInfo.rangeIterator(false);
while (iter.hasNext())
{
    RangeTombstone rt = iter.next();
    indexers.forEach(indexer -> indexer.rangeTombstone(rt));
}
```

---

## How to Spot

Be cautious whenever a stateful iterator is accessed inside a lambda passed to `forEach` or a stream operation. The lambda captures the variable holding the iterator, not the current element; if there are multiple consumers or the lambda is called more than once, `next()` will advance past unintended elements. The safe pattern is always to call `next()` exactly once per loop iteration, store the result in a local variable, and pass that variable into downstream consumers.

---

## Test

The new `rangeTombstoneTest` creates a partition with a range tombstone, builds two secondary indexes, indexes the partition, and asserts that both indexes received identical range tombstone lists, directly catching the divergence caused by the bug.
