# Bug 1581: Fix Heap Size Calculation for CompoundSparseCellName

**Commit**: c33e77ae77 · **Ticket**: CASSANDRA-7421
**Subsystem**: src/java/org/apache/cassandra/db/composites/CompoundSparseCellName.java (and related composites/cells)
**Category**: Wrong constant / default value

---

## Root Cause

`CompoundSparseCellName` and its inner class `WithCollection` did not override `unsharedHeapSize()` or `unsharedHeapSizeExcludingData()`, so they fell through to `CompoundComposite`'s implementation which used the wrong `HEAP_SIZE` constant (measured on a plain `CompoundComposite` instance, not a `CompoundSparseCellName`). The `WithCollection` variant additionally called `super.unsharedHeapSize()` which double-counted overhead. Throughout the hierarchy the method had also been misnamed `excessHeapSizeExcludingData` instead of `unsharedHeapSizeExcludingData`, preventing correct dispatch.

### Before / After

```java
// BEFORE – CompoundSparseCellName had no override; fell through to wrong base measurement
// WithCollection.unsharedHeapSize() called super (double-counted)
public long unsharedHeapSize()
{
    return super.unsharedHeapSize() + ObjectSizes.sizeOnHeapOf(collectionElement);
}
```

```java
// AFTER – CompoundSparseCellName overrides both methods with its own HEAP_SIZE
@Override
public long unsharedHeapSize()
{
    return HEAP_SIZE + ObjectSizes.sizeOnHeapOf(elements);
}
// WithCollection uses HEAP_SIZE directly, avoiding double-count via super
@Override
public long unsharedHeapSizeExcludingData()
{
    return HEAP_SIZE + ObjectSizes.sizeOnHeapExcludingData(elements)
           + ObjectSizes.sizeOnHeapExcludingData(collectionElement);
}
```

---

## How to Spot

When a subclass changes the object's layout (adds fields) relative to its parent, it must override all memory-measurement methods; inheriting them silently gives wrong answers. Review any class that adds fields but does not override `unsharedHeapSize` / similar.

---

## Test

No test added.
