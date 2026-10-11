# Bug 0097: TreeMap Race in CollectionVirtualTableAdapter Loses Virtual Table Rows

**Commit**: eb0c77ae1e · **Ticket**: CASSANDRA-20524
**Subsystem**: db/virtual
**Category**: Race condition / missing synchronization

---

## Root Cause

`CollectionVirtualTableAdapter.buildDataRangeIterator` populated a `NavigableMap` using a parallel stream (`StreamSupport.stream(data.spliterator(), true)`). The outer map was a `ConcurrentSkipListMap`, but the per-partition inner map was a plain (non-thread-safe) `TreeMap`. Multiple worker threads could simultaneously call `computeIfAbsent` for the same partition key, obtaining a reference to the same `TreeMap` and then concurrently calling `put` on it. `TreeMap.put` is not thread-safe: concurrent structural modifications can corrupt the tree and silently drop entries, causing rows to disappear from virtual table reads. The fix replaces the parallel stream with a sequential `for` loop and uses an ordinary `TreeMap` for the outer map.

### Before / After

```java
// BEFORE — outer map is thread-safe; inner TreeMap is not
NavigableMap<DecoratedKey, NavigableMap<Clustering<?>, Row>> partitionMap =
        new ConcurrentSkipListMap<>(DecoratedKey.comparator);
StreamSupport.stream(data.spliterator(), true)   // parallel
             .map(row -> makeRow(row, columnFilter))
             .filter(cr -> dataRange.keyRange().contains(cr.key.get()))
             .forEach(cr -> partitionMap
                     .computeIfAbsent(cr.key.get(), key -> new TreeMap<>(metadata.comparator))
                     .put(cr.clustering, cr.rowSup.get()));   // concurrent TreeMap.put
```

```java
// AFTER — sequential loop, plain TreeMap throughout
NavigableMap<DecoratedKey, NavigableMap<Clustering<?>, Row>> partitionMap =
        new TreeMap<>(DecoratedKey.comparator);
for (R row : data)
{
    CollectionRow cr = makeRow(row, columnFilter);
    if (dataRange.keyRange().contains(cr.key.get()))
        partitionMap.computeIfAbsent(cr.key.get(),
                                     key -> new TreeMap<>(metadata.comparator))
                   .put(cr.clustering, cr.rowSup.get());
}
```

---

## How to Spot

When a parallel stream writes into a shared mutable map, every map reachable from the shared structure must be thread-safe. Using a thread-safe outer container (`ConcurrentSkipListMap`) while creating non-thread-safe inner containers (`TreeMap`) in `computeIfAbsent` is a classic mixed-safety mistake. During review, trace every mutable data structure that a parallel stream's lambdas can reach and verify its thread safety independently.

---

## Test

No dedicated test added; the bug manifested as non-deterministic missing rows when scanning virtual tables backed by a collection.
