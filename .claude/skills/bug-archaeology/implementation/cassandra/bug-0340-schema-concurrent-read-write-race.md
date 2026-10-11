# Bug 0340: Concurrent Schema Read/Write Race in SchemaKeyspace

**Commit**: fa532a61f8 · **Ticket**: CASSANDRA-16996
**Subsystem**: src/java/org/apache/cassandra/schema/SchemaKeyspace.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`SchemaKeyspace.saveSystemKeyspacesSchema()`, `truncate()`, and `calculateSchemaDigest()` were all unsynchronized static methods that mutated or read shared schema tables. When called concurrently (e.g., a schema truncation racing with a digest computation during gossip reconciliation), they could interleave their reads and writes, producing partial or inconsistent schema state. The fix adds `synchronized` to all three methods. As part of this change, the table-name constants (`KEYSPACES`, `TABLES`, `COLUMNS`, etc.) were extracted into a dedicated `SchemaKeyspaceTables` class to decouple them from the stateful `SchemaKeyspace`, clarifying the locking scope.

### Before / After

```java
// BEFORE – unsynchronized, can interleave with concurrent callers
public static void saveSystemKeyspacesSchema() { ... }
public static void truncate() { ... }
static Pair<UUID, UUID> calculateSchemaDigest(Set<ByteBuffer> columnsToExclude) { ... }
```

```java
// AFTER – all three protected by the class monitor
public static synchronized void saveSystemKeyspacesSchema() { ... }
public static synchronized void truncate() { ... }
static synchronized Pair<UUID, UUID> calculateSchemaDigest(Set<ByteBuffer> columnsToExclude) { ... }
```

---

## How to Spot

Static utility methods that perform multi-step reads or writes to shared persistent state (system tables, in-memory registries) are often overlooked during code review when they appear to be "setup" routines. Anywhere a class exposes multiple static methods that each touch the same underlying tables without coordination, check whether they can be called concurrently and whether interleaving is safe.

---

## Test

`SchemaKeyspaceTest` was extended with additional tests covering concurrent schema operations; the existing test suite was updated to use `SchemaKeyspaceTables` constants.
