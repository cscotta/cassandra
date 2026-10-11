# Bug 0807: AttributeError in cqlsh When Table Metadata Is None

**Commit**: 13b7362494 · **Ticket**: CASSANDRA-12815
**Subsystem**: CQLSH
**Category**: Missing null / bounds check

---

## Root Cause
In `cqlsh.py`, the result-formatting path that resolves CQL types for display assumed `table_meta` was always a valid table metadata object and directly accessed `table_meta.keyspace_name`. When a result set had column type information but no associated table metadata (e.g., during certain system queries or queries where the metadata lookup failed), `table_meta` could be `None`, causing an `AttributeError`. Additionally, the subsequent `self.conn.metadata.keyspaces[ks_name]` used direct dict access instead of `.get()`, which would raise a `KeyError` for unknown keyspace names.

### Before / After
```python
# BEFORE
ks_meta = self.conn.metadata.keyspaces[table_meta.keyspace_name]

# AFTER
ks_name = table_meta.keyspace_name if table_meta else self.current_keyspace
ks_meta = self.conn.metadata.keyspaces.get(ks_name, None)
```

---

## How to Spot
In Python, any attribute access on a potentially-`None` object will raise `AttributeError` at runtime—there is no compile-time check. When reviewing code that resolves metadata objects (table, keyspace) from lookups that may return `None`, always add an explicit `if obj is not None` guard before accessing fields. Prefer `.get(key, default)` over `dict[key]` for dictionary lookups where the key may be absent, especially in display/formatting paths that run for every query result.

---

## Test
No test added.
