# Bug 1886: int/bigint Columns Read Back as Wrong Types in CassandraStorage (Pig Integration)

**Commit**: 3e7ebf84f9 · **Ticket**: CASSANDRA-6102
**Subsystem**: src/java/org/apache/cassandra/hadoop/pig (AbstractCassandraStorage, CassandraStorage, CqlStorage)
**Category**: Wrong serialization / deserialization

---

## Root Cause

The Pig `CassandraStorage` / `CqlStorage` Hadoop integration used `col.name()` directly as the key to look up a column's validator in the per-CF validator map. For CQL3 non-compact tables the column name stored on disk is a composite (clustering columns concatenated with the declared column name), so the raw `ByteBuffer` key never matched an entry in the validator map that was keyed by the bare column name. The result was that `int` and `bigint` columns fell through to the default validator path and were deserialized incorrectly (e.g., returned as `CHARARRAY` instead of `INTEGER`/`LONG` in Pig).

The fix introduced a `CfInfo` wrapper (alongside the existing `CfDef`) that carries two boolean flags — `cql3Table` and `compactCqlTable` — propagated through serialization into UDF context properties with a two-character prefix. In `columnToTuple()` the composite column name is split and only the final component (the bare column name) is used for validator lookup when the table is a non-compact CQL3 table. The `UUIDType` and `LexicalUUIDType` mappings were also corrected to return `DataType.CHARARRAY` (via `getString`) rather than invoking `compose()`, which produced unreadable binary.

### Before / After

```java
// BEFORE — raw composite column name used for validator lookup; fails for CQL3 tables
if (validators.get(col.name()) == null) {
    // falls through to default validator — wrong for int/bigint
} else {
    setTupleValue(pair, 1, cassandraToObj(validators.get(col.name()), col.value()));
}
```

```java
// AFTER — split composite name; use only the bare column name component
ByteBuffer colName;
if (cfInfo.cql3Table && !cfInfo.compactCqlTable) {
    ByteBuffer[] names = ((AbstractCompositeType) parseType(cfDef.comparator_type)).split(col.name());
    colName = names[names.length - 1];
} else {
    colName = col.name();
}
if (validators.get(colName) == null) { ... use default ... }
else { setTupleValue(pair, 1, cassandraToObj(validators.get(colName), col.value())); }
```

---

## How to Spot

When integrating with Cassandra's internal storage layer from a higher-level system (Pig, Spark, etc.), always verify that column name comparisons account for composite/compound name encoding in CQL3 tables. A round-trip test that writes `int`/`bigint` values via CQL and reads them back through the Pig loader will immediately reveal type mismatches.

---

## Test

No test added.
