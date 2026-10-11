# Bug 1203: SelectionColumnMapping Loses Column Order and Mishandles COUNT and No-Arg Functions

**Commit**: 2a294e45aa · **Ticket**: CASSANDRA-9636
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectionColumnMapping.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`SelectionColumnMapping` stored `ColumnSpecification` entries only as keys of a `LinkedHashMultimap`, so duplicate column expressions (e.g. `SELECT v1, v1`) were silently deduplicated and the result-set metadata had fewer columns than the query specified. Additionally, the `wildcard()` path in `Selection` never threaded through the `isCount` and `countAlias` parameters, so `COUNT(*)` queries produced result metadata containing all table columns instead of a single `count` column. Functions that accept no arguments (e.g. `now()`) mapped to a `null` column definition, which caused a `NullPointerException` when building mappings. The fix introduces a separate `ArrayList<ColumnSpecification>` to preserve insertion order and duplicates, adds a `countMapping()` factory that produces the correct single-column metadata, and guards `addMapping` against null column arguments.

### Before / After

```java
// BEFORE – duplicates lost; COUNT(*) emits all columns
private final LinkedHashMultimap<ColumnSpecification, CFDefinition.Name> columnMappings;

public List<ColumnSpecification> getColumnSpecifications() {
    return new ArrayList(columnMappings.keySet()); // keySet deduplicated
}

public static Selection wildcard(CFDefinition cfDef) {
    // ignores isCount / countAlias entirely
    return new SimpleSelection(all, true);
}
```

```java
// AFTER – separate list preserves duplicates; COUNT path builds correct spec
private final ArrayList<ColumnSpecification> columnSpecifications;
private final HashMultimap<ColumnSpecification, CFDefinition.Name> columnMappings;

public List<ColumnSpecification> getColumnSpecifications() {
    return Lists.newArrayList(columnSpecifications);
}

public static Selection wildcard(CFDefinition cfDef, boolean isCount, ColumnIdentifier countAlias) {
    SelectionColumnMapping mapping = isCount
        ? SelectionColumnMapping.countMapping(cfDef, countAlias)
        : SelectionColumnMapping.simpleMapping(cfDef);
    return new SimpleSelection(Lists.newArrayList(cfDef), mapping, true, isCount);
}
```

---

## How to Spot

Result-set metadata construction is bug-prone whenever the column specification list is derived from a map's key set: maps naturally deduplicate keys, so any query that legitimately selects the same expression twice will produce too few columns and confuse client drivers. Reviewers should confirm that the authoritative column-ordering structure is an ordered list, not a map key set. Separately, any method that accepts nullable domain objects should guard against null before inserting into a collection.

---

## Test

`SelectionColumnMappingTest` expanded with cases for `COUNT(*)`, `COUNT(1)` with and without alias, no-argument functions, nested functions, duplicate function expressions with and without aliases, `SELECT DISTINCT`, and multiple unaliased selections of the same column. Tests now execute queries end-to-end and verify both the column mapping and the actual result-set column list.
