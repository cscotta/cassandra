# Bug 0254: Memtable API Misconfiguration Throws at Schema Load Causing Node Failure

**Commit**: 9f58d76f38 · **Ticket**: CASSANDRA-18040
**Subsystem**: `schema/MemtableParams.java`, `schema/SchemaKeyspace.java`
**Category**: State not cleaned up

---

## Root Cause

When loading table metadata from the schema keyspace, `SchemaKeyspace` called `MemtableParams.get()` which throws a `ConfigurationException` if the memtable configuration key stored in the schema does not exist in `cassandra.yaml`. If a table was created on a node that had a custom memtable configured, and that table's schema was then replicated to another node that lacked the same memtable definition in its YAML, the uncaught `ConfigurationException` caused schema loading to abort, preventing the node from starting or the schema from being applied. The fix introduces `getWithFallback()` which catches the `ConfigurationException`, logs an error, and returns the default memtable params using the stored key as an identifier, allowing the schema to load without a mismatch while warning the operator.

### Before / After

```java
// BEFORE
.memtable(MemtableParams.get(row.has("memtable") ? row.getString("memtable") : null))
```

```java
// AFTER
.memtable(MemtableParams.getWithFallback(row.has("memtable") ? row.getString("memtable") : null))
// getWithFallback catches ConfigurationException and returns DEFAULT factory with stored key
```

---

## How to Spot

Any code path that deserializes schema from system tables and calls a method that depends on local `cassandra.yaml` configuration must handle the case where the local node's configuration does not match what was persisted. Schema loading must be resilient to missing local configuration because schema is replicated cluster-wide but YAML is node-local; a `ConfigurationException` thrown during schema hydration can render a node unable to start.

---

## Test

`AlterTest` in the distributed test suite verifies that a node can load a schema that references a memtable configuration name not present in its local YAML, confirming that the fallback path is taken and the node does not crash.
