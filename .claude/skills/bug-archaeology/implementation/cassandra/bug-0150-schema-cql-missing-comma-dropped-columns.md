# Bug 0150: Snapshot schema.cql Missing Comma After Dropped Columns

**Commit**: a5767a5834 · **Ticket**: CASSANDRA-19747
**Subsystem**: schema (TableMetadata)
**Category**: Logic error in condition

---

## Root Cause

`TableMetadata.toCqlString()` generates the `schema.cql` written into snapshots. When writing the list of dropped columns it uses two separate iterators — `iter` over the live columns and `iterDropped` over the dropped columns. After appending a dropped column's CQL, the code decided whether to add a trailing comma by checking `iter.hasNext()` (the live-column iterator) instead of `iterDropped.hasNext()` (the dropped-column iterator). As a result, when more than one column had been dropped, only the last dropped column was followed by a comma if there happened to be a live column remaining; any intermediate dropped column produced invalid CQL because the comma between consecutive dropped columns was omitted or misplaced.

### Before / After

```java
// BEFORE – checks the wrong iterator
if (!hasSingleColumnPrimaryKey || iter.hasNext())
    builder.append(',');
```

```java
// AFTER – checks the dropped-column iterator
if (!hasSingleColumnPrimaryKey || iterDropped.hasNext())
    builder.append(',');
```

---

## How to Spot

When two parallel iterators are advanced in the same loop body, any `hasNext()` call used to decide whether a separator is needed must reference the iterator that is currently being consumed. Using the wrong iterator produces an off-by-one in separator placement that only manifests when both iterators have remaining elements simultaneously, making the bug easy to miss in single-dropped-column tests.

---

## Test

Two new unit tests create a table, drop two columns, snapshot it, and assert the generated `schema.cql` matches the expected CQL string including all commas; one table has a composite primary key and the other a single-column primary key.
