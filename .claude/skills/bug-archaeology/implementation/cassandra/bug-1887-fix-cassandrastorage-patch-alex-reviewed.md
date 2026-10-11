# Bug 1887: int/bigint Columns Read Back as Wrong Types in CassandraStorage (Pig Integration, Branch Variant)

**Commit**: bc8e2475fa · **Ticket**: CASSANDRA-6102
**Subsystem**: src/java/org/apache/cassandra/hadoop/pig (AbstractCassandraStorage, CassandraStorage, CqlStorage)
**Category**: Wrong serialization / deserialization

---

## Root Cause

This is the earlier branch variant of the same CASSANDRA-6102 fix (see also Bug 1886). The Pig `CassandraStorage` integration used the raw on-disk composite column `ByteBuffer` as a validator map key; for CQL3 non-compact tables this composite key never matched the bare column name in the validator map, causing `int` and `bigint` values to be deserialized using the default validator rather than their declared typed validators.

Additionally the `columnToTuple()` method in this branch had to handle both standard and super column families through a single code path: if the column has sub-columns (super CF) it builds a `DataBag` of sub-column tuples; otherwise it applies the composite-aware validator lookup. The `UUIDType` and `LexicalUUIDType` types were also removed from the `CHARARRAY` branch in `getTypeForColumn()` so that they go through `compose()` rather than `getString()`, matching expected Pig type representations.

### Before / After

```java
// BEFORE — single validator lookup with raw composite name; incorrect for CQL3 tables
if (validators.get(col.name()) == null) {
    setTupleValue(pair, 1, cassandraToObj(marshallers.get(MarshallerType.DEFAULT_VALIDATOR), col.value()));
} else {
    setTupleValue(pair, 1, cassandraToObj(validators.get(col.name()), col.value()));
}
```

```java
// AFTER — split composite name for CQL3 non-compact tables, handle super columns separately
if (/* super column */) {
    ArrayList<Tuple> subcols = new ArrayList<>();
    for (IColumn subcol : col.getSubColumns())
        subcols.add(columnToTuple(subcol, cfInfo, parseType(cfDef.getSubcomparator_type())));
    pair.set(1, new DefaultDataBag(subcols));
} else {
    ByteBuffer colName = (cfInfo.cql3Table && !cfInfo.compactCqlTable)
        ? ((AbstractCompositeType) parseType(cfDef.comparator_type)).split(col.name())[last]
        : col.name();
    if (validators.get(colName) == null) { ... default ... }
    else { setTupleValue(pair, 1, cassandraToObj(validators.get(colName), col.value())); }
}
```

---

## How to Spot

When integrating with Cassandra's internal storage layer from a higher-level system (Pig, Spark, etc.), always verify that column name comparisons account for composite/compound name encoding in CQL3 tables. A round-trip test that writes `int`/`bigint` values via CQL and reads them back through the Pig loader will immediately reveal type mismatches. Super column handling should be tested independently from standard column handling.

---

## Test

No test added.
