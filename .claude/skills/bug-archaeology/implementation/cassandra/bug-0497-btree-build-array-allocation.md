# Bug 0497: BTree Build Unnecessary Collection Allocation

**Commit**: 01a091a041 · **Ticket**: CASSANDRA-15390
**Subsystem**: `src/java/org/apache/cassandra/utils/btree`
**Category**: Resource leak

---

## Root Cause

The BTree builder's internal `build(Object[] values, int count, ...)` path previously called `Arrays.asList(values).subList(0, count)` to produce a `List`, then called `source.iterator()` on that list at the start of `buildInternal`. This created two intermediate heap objects (an `ArrayList` wrapper and an `Iterator`) on every BTree construction, even though the caller already held a direct `Object[]`. On write-heavy paths (e.g., memtable row building), this allocation occurred on every partition mutation.

### Before / After

```java
// BEFORE — wraps an array into a List just to iterate it
return BTree.build(Arrays.asList(values).subList(0, count), UpdateFunction.noOp());
```

```java
// AFTER — new overload accepts the raw array directly via a generic IteratingFunction
public static <C, K extends C, V extends C> Object[] build(
        Object[] source, int size, UpdateFunction<K, V> updateF) {
    return buildInternal(source, ARRAY_FUNCTION, size, updateF);
}
// called as:
return BTree.build(values, count, UpdateFunction.noOp());
```

---

## How to Spot

Whenever an `Object[]` (or other concrete collection) is passed through an API that only accepts `Iterable` or `Iterator`, look for the wrapping step — `Arrays.asList`, `Collections.singleton`, etc. If the wrapping is done on a hot path, consider adding an overload that accepts the concrete type directly via a strategy object, eliminating the allocation without duplicating the core algorithm.

---

## Test

No test added (performance optimization, no behavioral change).
