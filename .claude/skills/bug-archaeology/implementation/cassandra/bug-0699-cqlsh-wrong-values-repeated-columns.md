# Bug 0699: cqlsh Displays Wrong Values When Same Column Selected Multiple Times

**Commit**: aaf2011287 · **Ticket**: CASSANDRA-13262
**Subsystem**: bin/cqlsh.py
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When formatting a result row for display, the code called `row.values()` to produce the sequence of cell values. A Python `Row` object from the driver is a named-tuple-like structure: `row.values()` returns values in the column-definition order, not in the SELECT clause column order. When a query repeats the same column more than once (e.g., `SELECT a, a, b FROM t`), the driver returns one result column per occurrence in the SELECT list, but iterating `row.values()` delivers each distinct column only once, so the repeated column is missing from the output and column alignment is shifted.

### Before / After

```python
# BEFORE — uses dict .values() order, ignoring repetitions in SELECT
formatted_values = [
    map(self.myformat_value, row.values(), cql_types)
    for row in result.current_rows
]
```

```python
# AFTER — indexes by column name, honouring SELECT clause order and repeats
formatted_values = [
    map(self.myformat_value, [row[c] for c in column_names], cql_types)
    for row in result.current_rows
]
```

---

## How to Spot

Whenever row values are collected by iterating `.values()` on a mapping-like driver row object rather than by explicitly indexing through the SELECT-clause column list, repeated column references in the query will silently produce the wrong output. A reviewer should check that the value sequence is derived from `column_names` (the SELECT output columns) rather than from the row's internal key set.

---

## Test

No test added in this commit.
