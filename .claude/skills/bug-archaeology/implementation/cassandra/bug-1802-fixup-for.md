# Bug 1802: BTree ReplaceFunction Called with null on New Insertion

**Commit**: 2fd2d89782 · **Ticket**: CASSANDRA-6271
**Subsystem**: src/java/org/apache/cassandra/utils/btree/BTree.java, AtomicBTreeColumns.java
**Category**: Missing null / bounds check

---

## Root Cause

The `ReplaceFunction<V>` interface had only one method, `apply(V replaced, V update)`, which was also used for pure insertions by passing `null` as `replaced`. Callers such as `NodeBuilder.copyInto` and the empty-tree fast-path in `BTree.update` invoked `replaceF.apply(null, v)` on new elements, meaning `AtomicBTreeColumns.UpdateFunction` received a `null` replaced-cell and attempted to call `replaced.dataSize()`, causing an NPE. The fix splits the interface: `ReplaceFunction` extends Guava's `Function<V,V>` and adds a single-arg `apply(V inserted)` method for new insertions, while the two-arg overload is reserved for actual replacements. All call sites for new elements are updated to use the single-arg variant.

### Before / After

```java
// BEFORE – called with null for new insertions
v = replaceF.apply(null, v);
// ... in AtomicBTreeColumns:
public Cell apply(Cell replaced, Cell update) {
    if (replaced == null) {
        indexer.insert(update); delta += update.dataSize();
    } else { ... }
    return transform.apply(update);  // always returned update, not reconciled
}
```

```java
// AFTER – separate method for insertions
public Cell apply(Cell inserted) {
    indexer.insert(inserted); delta += inserted.dataSize();
    return transform.apply(inserted);
}
public Cell apply(Cell replaced, Cell update) {
    Cell reconciled = update.reconcile(replaced, allocator);
    ...
    return transform.apply(reconciled);  // now returns reconciled cell
}
```

---

## How to Spot

Passing `null` as a sentinel value to represent "no previous element" in a two-arg callback is a code smell—null is not a valid domain object and callers are forced to add null guards that are easy to forget. Prefer separate single-arg and two-arg methods (or an `Optional`) to make the distinction explicit.

---

## Test

No test added.
