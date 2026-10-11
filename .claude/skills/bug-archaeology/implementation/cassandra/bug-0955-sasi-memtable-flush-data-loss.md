# Bug 0955: SASI index loses in-flight writes during memtable flush

**Commit**: 48815d4a18 · **Ticket**: CASSANDRA-11159
**Subsystem**: Secondary Indexes / SASI
**Category**: State not cleaned up

---

## Root Cause
When a Cassandra memtable flush was initiated, `Tracker.switchMemtable()` replaced the active memtable but only fired a `MemtableRenewedNotification` (for the truncate path). For the normal flush path, no notification was sent to SASI at all, so `SASIIndex`/`ColumnIndex` never knew the old in-memory index was about to be flushed. The old `IndexMemtable` was immediately discarded, taking with it all entries written since the last SSTable compaction. Queries running during the flush window could therefore miss recently-written rows.

Additionally, SASI's `QueryController` held a `RefViewFragment` that pinned a snapshot of SSTables at query-start, and the scope filter was applied inside `View.match()` using that snapshot. This tight coupling was also removed — the scope check was moved to an `applyScope()` helper in `QueryController`, and the `RefViewFragment` was dropped in favor of a lightweight range check against the `DataRange` from the command.

The fix introduces two new notification types — `MemtableSwitchedNotification` and `MemtableDiscardedNotification` — and a `pendingFlush` map in `ColumnIndex` that holds the pre-switch `IndexMemtable` until the flush completes. `searchMemtable()` now unions the current and all pending index memtables so no in-flight writes are missed.

### Before / After
```java
// BEFORE (Tracker.switchMemtable)
Pair<View, View> result = apply(View.switchMemtable(newMemtable));
if (truncating)
    notifyRenewed(newMemtable);
// no notification for non-truncate path

// AFTER
Pair<View, View> result = apply(View.switchMemtable(newMemtable));
if (truncating)
    notifyRenewed(newMemtable);
else
    notifySwitched(result.left.getCurrentMemtable()); // notify SASI of the old memtable

// BEFORE (ColumnIndex.switchMemtable)
public void switchMemtable() {
    memtable.set(new IndexMemtable(this));
}

// AFTER
public void switchMemtable(Memtable parent) {
    pendingFlush.putIfAbsent(parent, memtable.getAndSet(new IndexMemtable(this)));
}
public void discardMemtable(Memtable parent) {
    pendingFlush.remove(parent);
}
public RangeIterator<Long, Token> searchMemtable(Expression e) {
    RangeIterator.Builder<Long, Token> builder = new RangeUnionIterator.Builder<>();
    builder.add(getCurrentMemtable().search(e));
    for (IndexMemtable memtable : getPendingMemtables())
        builder.add(memtable.search(e));
    return builder.build();
}
```

---

## How to Spot
Any secondary-index implementation that maintains an in-memory index keyed by the active `Memtable` object must listen to both the switch (pre-flush) and discard (post-flush) lifecycle events. If only the truncate notification is handled, writes that arrive between the switch and the on-disk flush are silently dropped from in-memory index searches. Review all `INotificationConsumer.handleNotification()` implementations and ensure they cover `MemtableSwitchedNotification` and `MemtableDiscardedNotification` in addition to `MemtableRenewedNotification`.

---

## Test
`SASIIndexTest.testIndexMemtableSwitching()` — verifies that data written before a flush is still visible during the switch (pending state) and is correctly removed after the discard notification, and that truncate via `switchMemtable()` (no-arg) immediately discards all in-memory data.
