# Bug 0917: is_dense wrongly recalculated after Thrift table updates

**Commit**: e5c4027800 · **Ticket**: CASSANDRA-11502
**Subsystem**: Schema / Thrift compatibility
**Category**: Logic error in condition

---

## Root Cause
When a table was updated via Thrift, `ThriftConversion.fromThrift()` preserved "CQL metadata" columns (PARTITION_KEY, CLUSTERING_COLUMN, COMPACT_VALUE) by skipping only those that `isPartOfCellName()` returned true. This check was too broad: it retained CLUSTERING_COLUMN and COMPACT_VALUE definitions from the old schema even after the table's density (is_dense flag) had changed. Later, `CFMetaData.rebuild()` recalculated `isDense` from the now-stale set of column definitions and arrived at the wrong answer, making a previously-dense table appear sparse or vice versa. The fix computes `isDense` early from just the regular columns and the comparator, then uses it to selectively drop the now-inappropriate CLUSTERING_COLUMN and COMPACT_VALUE metadata, and finally stamps the computed value onto the new `CFMetaData` so that `rebuild()` cannot override it.

### Before / After
```java
// BEFORE – kept all non-isPartOfCellName() defs regardless of density change
for (ColumnDefinition def : previousCQLMetadata) {
    if (def.isPartOfCellName())
        continue;
    if (def.kind == PARTITION_KEY && hasKeyAlias)
        continue;
    defs.add(def);
}
CFMetaData newCFMD = new CFMetaData(ks, name, cfType, comparator, cfId);

// AFTER – compute isDense first, then filter correctly
boolean isDense = CFMetaData.calculateIsDense(fullRawComparator, defs);
for (ColumnDefinition def : previousCQLMetadata) {
    if (def.kind == REGULAR)                               continue;
    if (def.kind == PARTITION_KEY && hasKeyAlias)          continue;
    if (def.kind == COMPACT_VALUE && !isDense)             continue;
    if (def.kind == CLUSTERING_COLUMN && !isDense
        && (cfType == Standard || def.position() != 0))   continue;
    defs.add(def);
}
CFMetaData newCFMD = new CFMetaData(ks, name, cfType, comparator, cfId).isDense(isDense);
```

---

## How to Spot
Schema migration code that copies column definitions from an old table to a new one must re-evaluate the derived flags (`isDense`, `isCompound`) based on the new column set before deciding which old metadata entries to keep. If the density flag is derived inside `rebuild()` after all definitions have already been assembled, earlier filtering decisions that depend on density will be made against stale state.

---

## Test
`LegacySchemaTablesTest` was significantly extended with new test cases covering Thrift-updated tables where the density changes.
