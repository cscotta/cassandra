# Bug 0853: Super Column Cell Name Encoding Puts Wrong Subcomparator Value (Backport)

**Commit**: ae816cb84f · **Ticket**: CASSANDRA-12335
**Subsystem**: Storage Engine / Legacy Layout
**Category**: Wrong serialization / deserialization

---

## Root Cause
This is a backport of the same fix as Bug 0852 to a different branch.  `LegacyLayout.encodeCellName()` used the generic dense/non-dense composite-building logic for super-column tables, placing `columnName` rather than `collectionElement` at the subcomparator position in the composite for map-column cells.  See Bug 0852 for full details.

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
See Bug 0852.  When cherry-picking a fix across branches, verify the root cause exists on every target branch and that the surrounding context (method signatures, class names) still matches.

---

## Test
No test added.
