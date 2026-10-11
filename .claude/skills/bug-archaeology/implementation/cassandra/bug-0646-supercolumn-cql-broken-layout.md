# Bug 0646: CQL Queries Broken for SuperColumn Tables

**Commit**: ce8c9b559f · **Ticket**: CASSANDRA-12373
**Subsystem**: cql3, config/CFMetaData, db/CompactTables, schema/LegacySchemaMigrator
**Category**: Logic error in condition

---

## Root Cause

SuperColumn tables (a Thrift legacy concept) have a two-level column layout that does not map directly to the CQL storage model. The CQL layer had only partial, ad-hoc handling for this layout scattered across `CompactTables`, `CFMetaData`, and `LegacySchemaMigrator`, with several incorrect assumptions: `calculateIsDense` treated super-column tables the same as compact tables (returning `false` when no compact-value column was found, which broke `isDense` detection); `LegacySchemaMigrator` skipped the reconstruction of the virtual `superCfKeyColumn` and `superCfValueColumn` that CQL statements require in order to translate user-visible column names to the internal map-key / map-value representation; and `SelectStatement`, `UpdateStatement`, and `DeleteStatement` had no awareness of the CQL-to-internal-layout translation needed. The fix introduced a dedicated `SuperColumnCompatibility` class to centralize all the translation logic and wired it into `CFMetaData.rebuild()`, `LegacySchemaMigrator`, and the CQL statement layer.

### Before / After

```java
// BEFORE: CFMetaData.rebuild() — no super-column-aware path
this.comparator = new ClusteringComparator(extractTypes(clusteringColumns));
// columnMetadata built from all columns without virtual key/value columns

// AFTER: CFMetaData.rebuild() — super-column branch reconstructs virtual columns
if (isSuper() && isDense()) {
    superCfKeyColumn = SuperColumnCompatibility.getSuperCfKeyColumn(...);
    superCfValueColumn = SuperColumnCompatibility.getSuperCfValueColumn(...);
    // virtual columns inserted into columnMetadata for CQL name resolution
}
```

```java
// BEFORE: LegacySchemaMigrator.calculateIsDense — super tables returned false
if (isSuper) {
    for (UntypedResultSet.Row row : defs)
        if (row.getString("column_name").isEmpty())
            return false;
}
// AFTER: super tables always return true (they are always dense)
if (isSuper)
    return true;
```

---

## How to Spot

When legacy table formats require translation between an exposed CQL schema and an internal storage layout, every code path that reconstructs or interprets column metadata must handle the legacy format explicitly. The failure mode is subtle: a partially-wired abstraction appears to work for normal CQL tables but silently misbehaves for the edge-case layout. Reviewers should check that any `isSuper()` / `isDense()` branch in schema reconstruction, comparator construction, and CQL statement preparation is consistent with the actual physical layout, and that virtual/derived columns needed by the CQL layer are reliably reconstructed after every schema reload.

---

## Test

`ThriftIntegrationTest` added (942 lines) covering SELECT, INSERT, UPDATE, and DELETE operations against SuperColumn tables via CQL after migration from the legacy Thrift schema, asserting correct row content throughout.
