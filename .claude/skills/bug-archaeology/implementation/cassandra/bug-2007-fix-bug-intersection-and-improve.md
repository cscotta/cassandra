# Bug 2007: Incorrect SSTable Intersection Check and Off-by-One in Composite Min/Max Column Collection

**Commit**: 1ea5059fe3 · **Ticket**: CASSANDRA-5600
**Subsystem**: src/java/org/apache/cassandra/db/marshal/CompositeType.java, src/java/org/apache/cassandra/io/sstable/ColumnNameHelper.java
**Category**: Logic error in condition

---

## Root Cause

Two related bugs affected composite type SSTable optimization. First, `CompositeType.intersects()` used `deconstructAndExpand()` to split composite slice bounds, which pads missing components with empty buffers and relies on a type count that assumed `ColumnToCollectionType` was already stripped — but the assertion checked the wrong size, causing failures for tables with collection columns. The fix uses `split()` directly and handles missing components inline.

Second, `ColumnNameHelper.maxComponents()` and `minComponents()` iterated only up to `minSeen.size()`, silently discarding components from a longer `candidate` when a shorter list had been seen first. This under-counted the tracked min/max column boundaries, corrupting the SSTable metadata used to skip irrelevant SSTables during reads. The fix introduces `getTypeCount()` to cap collection at the correct number of actual types, and fills in trailing components from the longer list when sizes differ.

### Before / After

```java
// BEFORE (ColumnNameHelper.maxComponents)
for (int i = 0; i < maxSeen.size(); i++) {
    AbstractCompositeType.CompositeComponent component = components.get(i);
    retList.add(ColumnNameHelper.max(maxSeen.get(i), component.value, component.comparator));
}
```

```java
// AFTER
int minSize = Math.min(typeCount, Math.min(components.size(), maxSeen.size()));
int maxSize = Math.min(typeCount, biggest.size());
for (int i = 0; i < minSize; i++)
    retList.add(ColumnNameHelper.max(maxSeen.get(i), components.get(i), ct.types.get(i)));
for (int i = minSize; i < maxSize; i++)
    retList.add(biggest.get(i));
```

---

## How to Spot

When merging two lists element-wise, always consider the case where the lists differ in size. Iterating only up to `minSeen.size()` silently truncates the result when the candidate is longer. Also watch for code that computes a type count from a comparator without accounting for special trailing types like `ColumnToCollectionType`.

---

## Test

No test added.
