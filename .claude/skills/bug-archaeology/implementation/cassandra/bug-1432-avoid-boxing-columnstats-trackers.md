# Bug 1432: Avoid Boxing in ColumnStats Min/Max Trackers

**Commit**: f2673082ce · **Ticket**: CASSANDRA-8109
**Subsystem**: src/java/org/apache/cassandra/io/sstable/ColumnStats.java
**Category**: Wrong constant / default value

---

## Root Cause

`ColumnStats` used generic `MinTracker<T extends Comparable<T>>` and `MaxTracker<T extends Comparable<T>>` classes to track min/max timestamps (long) and deletion times (int). Because Java generics require object types, every primitive value was auto-boxed to `Long` or `Integer` on each `update()` call and then unboxed again on comparison. These trackers are called for every cell written during compaction and SSTable flushes, making the boxing overhead occur in a hot path that processes millions of cells.

### Before / After

```java
// BEFORE
public static class MinTracker<T extends Comparable<T>> {
    private T value;
    public void update(T value) {
        if (value.compareTo(this.value) < 0)  // autoboxed Long comparison
            this.value = value;
    }
}
// Used as:
ColumnStats.MinTracker<Long> minTimestampTracker = new ColumnStats.MinTracker<>(Long.MIN_VALUE);
ColumnStats.MaxTracker<Integer> maxDeletionTimeTracker = new ColumnStats.MaxTracker<>(Integer.MAX_VALUE);
```

```java
// AFTER
public static class MinLongTracker {
    private long value;
    public void update(long value) {
        if (value < this.value)  // primitive comparison, no boxing
            this.value = value;
    }
}
// Used as:
ColumnStats.MinLongTracker minTimestampTracker = new ColumnStats.MinLongTracker(Long.MIN_VALUE);
ColumnStats.MaxIntTracker maxDeletionTimeTracker = new ColumnStats.MaxIntTracker(Integer.MAX_VALUE);
```

---

## How to Spot

When you see generic tracker or accumulator classes parameterized with boxed primitive types (`Long`, `Integer`, `Double`) and those classes are used in tight loops over data — compaction, serialization, index building — check whether the generics can be replaced with primitive-typed specializations. The JVM does not eliminate boxing/unboxing across generic boundaries even when the type argument is always the same primitive wrapper, so any `Comparable<T>` comparison via `.compareTo()` will allocate objects in hot paths.

---

## Test

No test added.
