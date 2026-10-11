# Bug 1754: Fix AIOOBE When Concurrently Accessing ArrayBackedSortedColumns

**Commit**: d5c3d2fa56 · **Ticket**: CASSANDRA-6742
**Subsystem**: src/java/org/apache/cassandra/db/ArrayBackedSortedColumns.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`ArrayBackedSortedColumns.sortCells()` was unsynchronized and updated both `size` and `sortedSize` at non-atomic intermediate steps. A concurrent reader calling `getColumnCount()` or iterating the cells array could observe `size` mid-update while the internal `cells[]` array was still being compacted, leading to an `ArrayIndexOutOfBoundsException`. The fix makes `sortCells()` `synchronized` and adds a guard at the top to return immediately if the sort is already complete, preventing two threads from sorting at the same time. It also sets `sortedSize = -1` transiently during the merge to avoid a false-positive short-circuit, then restores it to `size` only after the merge is fully complete. Additionally, `internalAppend` no longer increments `sortedSize` (which was an incorrect shortcut), leaving `sortedSize` authoritative only after a full sort.

### Before / After

```java
// BEFORE
private void sortCells() {
    // no synchronization, no re-entry guard
    ...
    size = sortedSize = pos; // updated together mid-merge
    ...
}

private void internalAppend(Cell cell) {
    cells[size] = cell;
    size++;
    sortedSize++; // incorrectly tracks unsorted appends as sorted
}
```

```java
// AFTER
private synchronized void sortCells() {
    if (size == sortedSize) return; // guard: already sorted
    ...
    sortedSize = -1; // sentinel during merge
    size = pos;
    ...
    sortedSize = size; // set only after merge complete
}

private void internalAppend(Cell cell) {
    cells[size] = cell;
    size++;
    // sortedSize NOT incremented here
}
```

---

## How to Spot

When a class maintains two correlated counters (`size` and `sortedSize`) that must stay consistent, any method that temporarily makes them inconsistent must be synchronized end-to-end. Look for paired field updates that are not atomic and verify that concurrent readers cannot observe the intermediate state.

---

## Test

No test added.
