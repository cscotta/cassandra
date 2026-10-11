# Bug 1343: Multicolumn Relations With Secondary Indexes on Some Clustering Columns Return Wrong Results

**Commit**: 9649594c76 · **Ticket**: CASSANDRA-8275
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`SelectStatement` tracked column names that needed to be converted into `IndexExpression` objects using a plain `Set<CFDefinition.Name>`. When a multicolumn relation such as `(b, c) = (1, 1)` involved a clustering column that happened to carry a secondary index, the code tried to call `restriction.values(variables)` and extract a single value as if it were a single-column restriction, then threw `InvalidRequestException` or silently returned wrong results. The core problem was that the set carried no information about whether a given column's index was actually usable for the relation, so the pruning logic that removes clustering columns from the `IndexExpression` list used a blanket `removeAll`, incorrectly dropping columns that did need index expressions.

### Before / After

```java
// BEFORE — Set with no per-column index-usability flag
private final Set<CFDefinition.Name> restrictedNames = new HashSet<>();
// pruning removed all clustering columns regardless of whether they were indexed
if (!stmt.usesSecondaryIndexing)
    stmt.restrictedNames.removeAll(cfDef.clusteringColumns());
```

```java
// AFTER — Map records whether the column's own index is usable for the relation
private final Map<CFDefinition.Name, Boolean> restrictedNames = new HashMap<>();
// only remove clustering columns whose index cannot serve the relation
for (Name clusteringColumn : cfDef.clusteringColumns()) {
    Boolean indexed = stmt.restrictedNames.get(clusteringColumn);
    if (indexed == null) break;
    if (!indexed)
        stmt.restrictedNames.remove(clusteringColumn);
}
```

```java
// BEFORE — multicolumn restriction treated as single-column, wrong value extracted
List<ByteBuffer> values = restriction.values(variables);
if (values.size() != 1)
    throw new InvalidRequestException("IN restrictions are not supported on indexed columns");
ByteBuffer value = values.get(0);

// AFTER — multicolumn restriction extracts the value for the specific column position
if (restriction.isMultiColumn()) {
    List<ByteBuffer> values = restriction.values(variables);
    value = values.get(name.position);
} else {
    List<ByteBuffer> values = restriction.values(variables);
    if (values.size() != 1)
        throw new InvalidRequestException("IN restrictions are not supported on indexed columns");
    value = values.get(0);
}
```

---

## How to Spot

Whenever a collection is used to remember a set of "things to process later", and items are removed from it based on a boolean condition, verify that the collection records the data needed to evaluate that condition. Replacing a `Set` with a `Map<K, Boolean>` (or a richer value type) is a common fix. Also look for any code that calls `.values()` on a restriction without first checking `isMultiColumn()`: multicolumn restrictions return one value per column, not one value per restriction.

---

## Test

`MultiColumnRelationTest` and `SingleColumnRelationTest` extended with queries combining multicolumn relations and secondary indexes on clustering and non-clustering columns.
