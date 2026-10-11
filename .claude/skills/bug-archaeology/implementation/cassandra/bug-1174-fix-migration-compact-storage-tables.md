# Bug 1174: pk-only Compact Storage Tables Incorrectly Skipped During Migration

**Commit**: 2d55c1e846 · **Ticket**: CASSANDRA-9874
**Subsystem**: src/java/org/apache/cassandra/schema/LegacySchemaMigrator.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

Pre-3.0 dense (COMPACT STORAGE) tables that have only primary-key columns and no regular columns were stored in the legacy schema with a single phantom column whose name is an empty string and whose type is `compact_value`. `LegacySchemaMigrator.checkNeedsUpgrade()` decided whether to call `addDefinitionForUpgrade()` — which synthesises the proper `EmptyType` regular column — by testing `hasKind(defs, ColumnDefinition.Kind.REGULAR)`. The trouble is that `deserializeKind("compact_value")` returns `REGULAR`, so the phantom empty-name column was counted as a real regular column. The migrator therefore concluded no upgrade was needed, skipped `addDefinitionForUpgrade()`, and then proceeded to include the empty-name column directly in the column definitions, producing a table with a garbage column instead of the correct schema. The fix introduces `isEmptyCompactValueColumn()` and `hasRegularColumns()` that explicitly exclude the phantom column from the REGULAR count, and also skips it during column list construction so that `addDefinitionForUpgrade()` can insert the correct typed placeholder.

### Before / After

```java
// BEFORE: phantom compact_value column counted as a real REGULAR column
return !hasKind(defs, ColumnDefinition.Kind.REGULAR);
```

```java
// AFTER: phantom column excluded; upgrade path correctly triggered
private static boolean hasRegularColumns(UntypedResultSet columnRows) {
    for (UntypedResultSet.Row row : columnRows) {
        if (isEmptyCompactValueColumn(row))
            return false; // force upgrade path
        if (deserializeKind(row.getString("type")) == ColumnDefinition.Kind.REGULAR)
            return true;
    }
    return false;
}

private static boolean isEmptyCompactValueColumn(UntypedResultSet.Row row) {
    return "compact_value".equals(row.getString("type")) && row.getString("column_name").isEmpty();
}
```

---

## How to Spot

Migration code that reuses a general-purpose classifier (`deserializeKind`) for a decision that requires special-casing a sentinel value is vulnerable to silent misclassification. When the legacy storage format uses a synthetic record with an empty key to represent "no data," any code that simply counts records of a given kind without first excluding these sentinel rows will over-count. Review all `hasKind`-style helpers in migration paths and ask whether any sentinel or placeholder rows could match the kind being queried.

---

## Test

A `compact_pkonly` table is added to `LegacySchemaMigratorTest` to cover the primary-key-only dense compact table case. The test helper is also updated to serialise the empty-name column correctly so the round-trip through the fake legacy schema produces the phantom row that triggers the bug path.
