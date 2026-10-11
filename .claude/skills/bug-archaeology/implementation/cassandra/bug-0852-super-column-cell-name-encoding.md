# Bug 0852: Super Column Cell Name Encoding Puts Wrong Subcomparator Value

**Commit**: 676b6a891d · **Ticket**: CASSANDRA-12335
**Subsystem**: Storage Engine / Legacy Layout
**Category**: Wrong serialization / deserialization

---

## Root Cause
`LegacyLayout.encodeCellName()` computed the composite byte buffer for super-column cell names using the same logic as normal dense/non-dense tables.  For super columns the last component of the composite must be the *subcomparator* value (the "column" in Thrift terms), but the existing code always placed `columnName` at `values[clusteringSize]` and `collectionElement` at `values[clusteringSize+1]` regardless of table type.  For a super-column map cell, `columnName` is the sentinel `SUPER_COLUMN_MAP_COLUMN` and the real subcomparator value lives in `collectionElement` — so the serialized composite had the wrong component in the final position.  The fix detects `metadata.isSuper()` and uses `collectionElement` when the column name is the map-column sentinel, otherwise uses `columnName` directly.

### Before / After
```java
// BEFORE – always uses the generic non-dense path, wrong for super columns
if (!metadata.isDense())
    values[clusteringSize] = columnName;
if (collectionElement != null)
    values[clusteringSize + 1] = collectionElement;

// AFTER – super-column path selects the correct subcomparator component
if (metadata.isSuper())
{
    values[clusteringSize] = columnName.equals(CompactTables.SUPER_COLUMN_MAP_COLUMN)
                           ? collectionElement
                           : columnName;
}
else
{
    if (!metadata.isDense())
        values[clusteringSize] = columnName;
    if (collectionElement != null)
        values[clusteringSize + 1] = collectionElement;
}
```

---

## How to Spot
Legacy-layout code has three overlapping table modes (dense, non-dense, super) and the conditions governing them interact in non-obvious ways.  Any path that assembles a composite byte buffer from clustering values, column names, and collection elements should be reviewed for each table mode independently.  A missing `isSuper()` branch is easy to overlook because super-column tables are rare in modern clusters.

---

## Test
No test added.
