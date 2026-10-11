# Bug 0509: cqlsh DESCRIBE Omits Virtual Keyspaces

**Commit**: 4e0fccc3ed · **Ticket**: CASSANDRA-15576
**Subsystem**: `bin/cqlsh.py`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`get_keyspace_names()` returned only the keyspaces stored in the driver's `conn.metadata.keyspaces` dictionary, which is populated from system schema tables. Virtual keyspaces (e.g., `system_views`) are not replicated to schema tables and so are absent from the driver metadata. The method already populated a separate `self.virtual_keyspaces` dict via `init_virtual_keyspaces_meta()`, but then failed to include that dict's keys in the returned list. As a result, `DESCRIBE KEYSPACES` and tab-completion silently omitted all virtual keyspaces. A companion bug in `get_connection_versions` read `native_protocol_version` from `system.local` (which returns a string like `"4"`) rather than using the actual negotiated protocol version from the driver connection object.

### Before / After

```python
# BEFORE
def get_keyspace_names(self):
    if self.virtual_keyspaces is None:
        self.init_virtual_keyspaces_meta()
    return list(map(str, list(self.conn.metadata.keyspaces.keys())))
    # virtual_keyspaces silently dropped

# BEFORE (connection version)
'protocol': result['native_protocol_version'],  # string from system.local
```

```python
# AFTER
def get_keyspace_names(self):
    if self.virtual_keyspaces is None:
        self.init_virtual_keyspaces_meta()
    return list(map(str, list(self.conn.metadata.keyspaces.keys())
                          + list(self.virtual_keyspaces.keys())))

# AFTER
'protocol': self.conn.protocol_version,  # actual negotiated version
```

---

## How to Spot

When code maintains two separate collections for different categories of the same entity type (here: real vs. virtual keyspaces) and then builds a result list from only one of them, it is a classic omission bug. Review every place that aggregates "all items" to confirm every source collection is included. Also watch for reading configuration or version values from data rows when the runtime object already exposes the authoritative value directly.

---

## Test

No test added.
