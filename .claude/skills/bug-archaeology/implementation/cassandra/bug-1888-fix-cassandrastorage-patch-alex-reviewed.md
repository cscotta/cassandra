# Bug 1888: Fix int/bigint in CassandraStorage

**Commit**: 639c01a350 · **Ticket**: CASSANDRA-6102
**Subsystem**: src/java/org/apache/cassandra/hadoop/pig
**Category**: Wrong serialization / deserialization

---

## Root Cause

In the Pig/Hadoop integration, `CassandraStorage` was looking up column validators using the raw composite column name instead of extracting the final component of the composite key. For CQL3 tables, Cassandra stores column names as composite byte buffers where the actual column identifier occupies the last component; using the full composite buffer as the validator-map key always missed and fell through to the default validator. This caused `int` and `bigint` CQL column types to be returned as `CHARARRAY` (string) instead of `INTEGER` or `LONG` respectively. Additionally, `LexicalUUIDType` and `UUIDType` were incorrectly mapped to `DataType.CHARARRAY` and had their values passed through `getString()` instead of `compose()`, losing the native type.

### Before / After

```java
// BEFORE
if (validators.get(col.name()) == null)
    // falls through to default marshaller
setTupleValue(pair, 1, cassandraToObj(validators.get(col.name()), col.value()));
```

```java
// AFTER
ByteBuffer colName;
if (cfInfo.cql3Table && !cfInfo.compactCqlTable) {
    ByteBuffer[] names = ((AbstractCompositeType) parseType(cfDef.comparator_type)).split(col.name());
    colName = names[names.length - 1];
} else {
    colName = col.name();
}
if (validators.get(colName) == null) { ... }
setTupleValue(pair, 1, cassandraToObj(validators.get(colName), col.value()));
```

---

## How to Spot

When validator/type lookups use a column name that may be a composite key, always check whether the comparator is composite and split out the relevant component before the map lookup. Any code that calls `validators.get(col.name())` on a CQL3 table without splitting is suspect.

---

## Test

No test added.
