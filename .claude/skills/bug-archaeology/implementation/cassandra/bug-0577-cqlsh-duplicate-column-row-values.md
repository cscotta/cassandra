# Bug 0577: cqlsh Displays Wrong Values When Same Column Selected Multiple Times

**Commit**: 62e48c5f3f · **Ticket**: CASSANDRA-13262
**Subsystem**: bin/cqlsh.py
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When formatting result rows, cqlsh iterated over `row.values()` (the dict's value view in insertion order) rather than indexing each column by name from the ordered `column_names` list. When a query repeats the same column — e.g. `SELECT col, col FROM t` — the row dict contains only one entry for that column name, so `row.values()` produced fewer values than `formatted_names`, causing the repeated column to display a value from the wrong position or be missing entirely.

### Before / After

```python
# BEFORE
formatted_values = [map(self.myformat_value, row.values()) for row in rows]
```

```python
# AFTER
formatted_values = [map(self.myformat_value,
                        [row[column] for column in column_names])
                    for row in rows]
```

---

## How to Spot

Any code that formats or displays tabular results by iterating a dict's `.values()` instead of looking up each key from the ordered column list is fragile to duplicate column names. The safe pattern is always to drive the loop from the authoritative ordered name list and index into the row object by name.

---

## Test

No test added.
