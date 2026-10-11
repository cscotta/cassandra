# Bug 1106: Fix Race on StorageService.doAuthSetup

**Commit**: 88b05ef4b0 · **Ticket**: CASSANDRA-10104
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`doAuthSetup` checks whether each auth keyspace table already exists before calling `maybeAddTable`. The original guard used `Schema.instance.getCFMetaData(ksName, cfName)` — a name-based lookup. When multiple nodes are started concurrently, a table can be added to the schema with one `cfName` but the lookup may miss it if the concurrent add arrived under a slightly different view of the schema or caused a cached name reference to be stale. The stable identifier for a table is its UUID (`cfId`), not its mutable name pair, so switching to `Schema.instance.getCF(table.cfId)` makes the existence check immune to name-based race windows.

### Before / After

```java
// BEFORE
if (Schema.instance.getCFMetaData(table.ksName, table.cfName) == null)
    maybeAddTable(table);
```

```java
// AFTER
if (Schema.instance.getCF(table.cfId) == null)
    maybeAddTable(table);
```

---

## How to Spot

Existence checks on mutable identifiers (names that can be renamed or looked up through mutable state) are fragile under concurrency. When an object has a stable immutable identity (UUID, cfId), prefer using that for existence checks, especially in initialization paths that can race with other nodes performing the same setup. Review any `getXByName` call in a concurrent context and ask whether an ID-based alternative exists.

---

## Test

No test added. The race is inherently timing-dependent and is addressed by using the stable identity field.
