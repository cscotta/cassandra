# Bug 1041: cqlsh COPY Import Breaks After Driver API Change

**Commit**: 9940892c3c · **Ticket**: n/a
**Subsystem**: cqlsh / client tooling
**Category**: Wrong constant / default value

---

## Root Cause
The Python driver's `ColumnMetadata` object renamed the attribute used to retrieve a column's type string from `typestring` to `cql_type`. The `COPY FROM` code path in `cqlsh.py` inside `ImportProcess.run()` continued to reference the old `typestring` attribute, causing an `AttributeError` at runtime whenever a COPY import was attempted after upgrading to the new driver version.

### Before / After
```python
# BEFORE
cqltypes = [table_meta.columns[name].typestring for name in self.columns]

# AFTER
cqltypes = [table_meta.columns[name].cql_type for name in self.columns]
```

---

## How to Spot
When a third-party library's metadata API is updated and the client code accesses an attribute by name (rather than through an accessor method), any rename in the library silently breaks all callers at runtime rather than at compile time. In review, look for direct attribute access on objects returned by external libraries — especially in Python code — and verify the attribute names against the library version being targeted. A `COPY FROM` integration test covering basic import would have caught this immediately.

---

## Test
No test added.
