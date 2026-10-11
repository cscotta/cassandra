# Bug 1582: Fix Heap Size Calculation for CompoundSparseCellName

**Commit**: d4833593be · **Ticket**: CASSANDRA-7421
**Subsystem**: src/java/org/apache/cassandra/db/composites/CompoundSparseCellName.java (and related composites/cells)
**Category**: Wrong constant / default value

---

## Root Cause

This is the same fix as bug 1581 (CASSANDRA-7421) applied to a different branch. `CompoundSparseCellName` and its `WithCollection` inner class did not override `unsharedHeapSize()` or `unsharedHeapSizeExcludingData()`, inheriting a base-class measurement taken on a `CompoundComposite` instance and therefore reporting incorrect heap sizes. Additionally the method was misnamed `excessHeapSizeExcludingData` across the interface hierarchy, breaking polymorphic dispatch. The `WithCollection.unsharedHeapSize()` also called `super`, which double-counted the base overhead.

### Before / After

```java
// BEFORE – no override in CompoundSparseCellName; wrong HEAP_SIZE inherited
// WithCollection double-counted via super()
public long unsharedHeapSize()
{
    return super.unsharedHeapSize() + ObjectSizes.sizeOnHeapOf(collectionElement);
}
public long excessHeapSizeExcludingData()   // wrong name – not called polymorphically
{
    return super.excessHeapSizeExcludingData() + ObjectSizes.sizeOnHeapExcludingData(collectionElement);
}
```

```java
// AFTER – CompoundSparseCellName overrides with correct HEAP_SIZE; WithCollection avoids super
@Override
public long unsharedHeapSizeExcludingData()
{
    return HEAP_SIZE + ObjectSizes.sizeOnHeapExcludingData(elements)
           + ObjectSizes.sizeOnHeapExcludingData(collectionElement);
}
```

---

## How to Spot

When a subclass adds instance fields, verify it overrides every method that measures object size. Also verify that interface method names exactly match their implementations — a mismatched name silently prevents overriding.

---

## Test

No test added.
