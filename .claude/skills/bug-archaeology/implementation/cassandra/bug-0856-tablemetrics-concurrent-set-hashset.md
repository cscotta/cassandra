# Bug 0856: TableMetrics allTableMetrics uses non-thread-safe HashSet

**Commit**: 52be7bac41 · **Ticket**: CASSANDRA-11823
**Subsystem**: Metrics / TableMetrics
**Category**: Race condition / missing synchronization

---

## Root Cause
`TableMetrics.allTableMetrics` is a `ConcurrentMap<String, Set<Metric>>`, but the value `Set` was created with `new HashSet<>()` — a type with no thread-safety guarantees. When a reporting thread (e.g., `GraphiteReporter`) iterates the set concurrently with another thread calling `register()`, which adds a metric to the set via `.add()`, a `ConcurrentModificationException` is thrown. Replacing `HashSet` with a `ConcurrentHashMap`-backed key set makes the collection safe for concurrent reads and writes.

### Before / After
```java
// BEFORE
boolean ret = allTableMetrics.putIfAbsent(name,  new HashSet<>()) == null;

// AFTER
boolean ret = allTableMetrics.putIfAbsent(name, ConcurrentHashMap.newKeySet()) == null;
```

---

## How to Spot
Whenever a `ConcurrentMap` stores a collection as a value, check whether the value collection itself is thread-safe. A `putIfAbsent` that inserts a plain `HashSet` or `ArrayList` is a red flag: the outer map operation is atomic, but subsequent `.add()` or iteration on the value object is not. Look for any place where metrics are registered at table/keyspace creation time — those paths can race with periodic metric reporters that iterate the same sets.

---

## Test
No dedicated test added. The fix is structural — the `ConcurrentHashMap.newKeySet()` set serialises concurrent mutations, preventing the `ConcurrentModificationException` observed in `GraphiteReporter`.
