# Bug 1430: Avoid Boxing in ColumnStats min/max Trackers

**Commit**: c774ea3463 · **Ticket**: CASSANDRA-8109
**Subsystem**: src/java/org/apache/cassandra/io/sstable/ColumnStats.java
**Category**: Wrong constant / default value

---

## Root Cause

`ColumnStats.MinTracker<T>` and `MaxTracker<T>` used a generic `Comparable<T>` type parameter to track minimum and maximum column timestamps and TTLs. Because the tracked values were primitive `long` and `int` quantities, every `update()` call incurred autoboxing from the primitive to its wrapper type (`Long`, `Integer`) and unboxing back. For hot compaction paths that process millions of cells, this generated significant GC pressure. The fix replaced the generic classes with concrete `MinLongTracker`, `MaxLongTracker`, and `MaxIntTracker` classes that operate directly on primitives, eliminating all boxing.

### Before / After

```java
// BEFORE – generic tracker forces boxing on every update
public static class MinTracker<T extends Comparable<T>> {
    public void update(T value) {
        if (value.compareTo(this.value) < 0)  // boxed comparison
            this.value = value;
    }
}
```

```java
// AFTER – primitive tracker, no boxing
public static class MinLongTracker {
    public void update(long value) {
        if (value < this.value)               // primitive comparison
            this.value = value;
    }
}
```

---

## How to Spot

Generic utility classes (`Tracker<T extends Comparable<T>>`, `Accumulator<T extends Number>`) that are always instantiated with a boxed primitive type (`Long`, `Integer`) are a sign of unnecessary boxing. Reviewers should check whether a simple primitive specialization (or a primitive collection library) would be more appropriate, especially in code on the write or compaction hot path where allocation rate matters.

---

## Test

No test added.
