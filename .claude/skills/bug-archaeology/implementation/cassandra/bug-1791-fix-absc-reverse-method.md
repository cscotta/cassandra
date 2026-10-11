# Bug 1791: Fix ABSC Reverse Iterator remove() Method

**Commit**: d43f84bb21 · **Ticket**: CASSANDRA-6629
**Subsystem**: src/java/org/apache/cassandra/db
**Category**: Logic error in condition

---

## Root Cause

The reverse iterator in `ArrayBackedSortedColumns` decremented `idx` inside `next()` (so after calling `next()` the index pointed one position behind the returned element). The `remove()` method then called `columns.remove(idx--)`, which removed the element one position earlier than the one just returned and also double-decremented the index, corrupting the iteration state. The fix introduces a `shouldCallNext` flag (mirroring the `Iterator` contract that `remove()` requires a preceding `next()` call) and changes `remove()` to delete `columns.get(idx + 1)` — the element that was most recently returned.

### Before / After

```java
// BEFORE
public IColumn next() {
    return columns.get(idx--);
}
public void remove() {
    columns.remove(idx--);
}
```

```java
// AFTER
public IColumn next() {
    shouldCallNext = false;
    return columns.get(idx--);
}
public void remove() {
    if (shouldCallNext)
        throw new IllegalStateException();
    columns.remove(idx + 1);
    shouldCallNext = true;
}
```

---

## How to Spot

When an iterator advances by pre-decrementing (or pre-incrementing) an index, `remove()` must reference `index + 1` (or `index - 1`) to reach the element that `next()` last returned — using the raw post-advance index removes the wrong element.

---

## Test

`ArrayBackedSortedColumnsTest.testRemove` — inserts four columns in both forward and reversed order, iterates the reverse view calling `remove()` after each `next()`, and asserts that exactly the expected elements are removed without skipping or double-removing.
