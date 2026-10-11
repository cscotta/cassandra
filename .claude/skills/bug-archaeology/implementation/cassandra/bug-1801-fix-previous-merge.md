# Bug 1801: Fix Secondary Index Updater Called with Wrong "Replaced" Cell

**Commit**: f6f50ddffe · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/AtomicBTreeColumns.java
**Category**: Logic error in condition

---

## Root Cause

During a BTree column merge, the `apply(Cell replaced, Cell update)` callback determined which cell won reconciliation and then swapped the arguments to `indexer.update()` based on the result: if `reconciled == update` it called `indexer.update(replaced, reconciled)`, otherwise it called `indexer.update(update, reconciled)`. The second branch incorrectly passes `update` as the old value when `update` lost reconciliation and `replaced` is actually the winning cell; the indexer would receive the wrong "before" value, corrupting secondary index entries. The fix unconditionally calls `indexer.update(replaced, reconciled)`, which is always correct: `replaced` is always the old cell and `reconciled` is always the new one.

### Before / After

```java
// BEFORE
Cell reconciled = update.reconcile(replaced, allocator);
if (reconciled == update)
    indexer.update(replaced, reconciled);
else
    indexer.update(update, reconciled);   // wrong: passes update as "old"
```

```java
// AFTER
Cell reconciled = update.reconcile(replaced, allocator);
indexer.update(replaced, reconciled);
```

---

## How to Spot

Whenever reconciliation can return either of its two inputs (winner-takes-all semantics), verify that downstream callbacks always receive the semantically stable "old" and "new" arguments rather than trying to identify which argument won—the identity of the winner does not determine which side was "before".

---

## Test

No test added.
