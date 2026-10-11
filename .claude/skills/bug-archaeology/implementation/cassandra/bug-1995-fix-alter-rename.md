# Bug 1995: ALTER RENAME Breaks Column Family Metadata After Schema Refactor

**Commit**: 67435b528d · **Ticket**: CASSANDRA-5702
**Subsystem**: src/java/org/apache/cassandra/config/CFMetaData.java, src/java/org/apache/cassandra/cql3/statements/AlterTableStatement.java, src/java/org/apache/cassandra/config/ColumnDefinition.java
**Category**: State not cleaned up

---

## Root Cause

A prior refactor (CASSANDRA-5125) introduced a `rebuild()` method to reconstruct derived `CFMetaData` state (partition key columns, clustering key columns, CQL view) from the canonical column metadata map. Before the fix, many code paths that mutated `CFMetaData` called an older `updateCfDef()` hook inline, but after the refactor those calls were removed or became inconsistent, leaving the object in a partially-initialised state after `ALTER RENAME`. `AlterTableStatement` performed renames on column definitions but never triggered a full rebuild, so the CQL-level view of the table remained stale. The fix ensures `rebuild()` is called at every construction and mutation exit point, and removes the `DEFAULT_KEY_NAME` constant in favour of explicit default alias strings that survive the rename.

### Before / After

```java
// BEFORE – factory methods returned un-rebuilt metadata
return cfmd;                      // fromCQL3Type path
...
.reloadSecondaryIndexMetadata(parent);  // copyOpts path
```

```java
// AFTER – every factory path ends with rebuild()
return cfmd.rebuild();
...
.reloadSecondaryIndexMetadata(parent)
.rebuild();
```

---

## How to Spot

When a class separates canonical mutable state from derived/cached state, every code path that modifies canonical state must trigger a rebuild of the derived state. Watch for factory or builder methods that return `this` or a new instance without a final normalization call, especially after a refactor that moves the normalization out of individual setters.

---

## Test

`CFMetaDataTest` was extended to cover the rename scenario.
