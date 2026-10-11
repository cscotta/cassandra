# Bug 2056: Compaction Unmark Set Diverges from Marked Set

**Commit**: a393072aac · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/ColumnFamilyStore.java
**Category**: State not cleaned up

---

## Root Cause

`markAllCompacting()` constructed an `Iterable<SSTableReader>` from `filterSuspectSSTables(getSSTables())`, which returns a lazy view over the live SSTable set. Because `getSSTables()` can change between the time `markCompacting` is called and the time the compaction task later calls `unmarkCompacting`, the lazy iterable could yield a different set of SSTables on re-iteration — meaning the tables that get unmarked may not be the same ones that were marked. The fix materialises the iterable into a `List` immediately so the identity of the marked set is frozen.

### Before / After

```java
// BEFORE
Iterable<SSTableReader> sstables = AbstractCompactionStrategy.filterSuspectSSTables(getSSTables());
boolean success = data.markCompacting(sstables);
```

```java
// AFTER
Iterable<SSTableReader> sstables = Lists.newArrayList(
    AbstractCompactionStrategy.filterSuspectSSTables(getSSTables()));
boolean success = data.markCompacting(sstables);
```

---

## How to Spot

Whenever you mark a resource set at time T and must unmark the same set at time T+N, capture the set in an eagerly evaluated collection (List, Set) immediately at mark time. Any lazy iterable or live view risks yielding a different result on re-iteration if the underlying source mutates.

---

## Test

No test added.
