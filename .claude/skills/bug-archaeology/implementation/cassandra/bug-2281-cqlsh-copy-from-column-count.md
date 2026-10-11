# Bug 2281: cqlsh COPY FROM Validates Row Width Against Input Column List, Not Actual Column Count

**Commit**: fc8e4b5656 · **Ticket**: CASSANDRA-4470

## Detection Rule

When a function returns a derived value computed from its inputs (e.g., the actual number of columns in a prepared statement), verify callers use the returned value rather than re-deriving it from the original input list, which may differ after normalization.

## Code Smell

`len(columns)` is used to validate CSV row width after `prep_import_insert` may have expanded or changed the column list; the function returns both the query and the column count, but callers ignore the returned count and re-check `len(columns)` from the outer scope.

## Tags

- scope: universal
- category: wrong-filter-result
- code-pattern: stale-derived-count-mismatch
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause

`prep_import_insert` builds an INSERT statement and returns the actual column count to use, but the caller discarded that count and re-evaluated `len(columns)` for row-width validation. When columns were not explicitly specified, the function resolved them from the table schema, making the actual column count differ from `len(columns)` passed in. The fix destructures the return value and uses the returned `numcol` for validation.

## Example

```python
# BEFORE
prepq = self.prep_import_insert(ks, cf, columns)
for rownum, row in enumerate(reader):
    if len(row) != len(columns):  # 'columns' may be empty list; actual count differs
        self.printerr(...)
```

```python
# AFTER
numcol, prepq = self.prep_import_insert(ks, cf, columns)
for rownum, row in enumerate(reader):
    if len(row) != numcol:        # use actual resolved column count
        self.printerr(...)
```
