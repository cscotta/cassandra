# Bug 0890: Super Column Schema Upgrade Fails with Non-Text Subcomparators

**Commit**: b671522d00 · **Ticket**: CASSANDRA-12023
**Subsystem**: Schema Migration / Legacy Compact Storage
**Category**: Wrong serialization / deserialization

---

## Root Cause
`CompactTables.columnDefinitionComparator()` accepted a `ColumnDefinition.Kind` enum value and used it to decide which comparator to apply to a column definition during legacy schema migration. For super-column tables the comparator for a `REGULAR` column should be the raw subcomparator (e.g., `LongType`, `IntegerType`, etc.), but a `compact_value` pseudo-kind — stored as the raw string `"compact_value"` in the legacy schema tables — was never deserialized into a `ColumnDefinition.Kind` and therefore fell through the comparator selection logic to `UTF8Type.instance` instead. The fix changes the method signature to accept the raw `String` kind directly, handles `"compact_value"` explicitly before any enum comparison, and switches the remaining comparisons to string equality, ensuring non-text subcomparators are preserved during the migration.

### Before / After
```java
// BEFORE — Kind enum cannot represent "compact_value"; falls through to wrong UTF8 branch
public static AbstractType<?> columnDefinitionComparator(
        ColumnDefinition.Kind kind, boolean isSuper,
        AbstractType<?> rawComparator, AbstractType<?> rawSubComparator) {
    if (isSuper)
        return kind == ColumnDefinition.Kind.REGULAR ? rawSubComparator : UTF8Type.instance;
    else
        return kind == ColumnDefinition.Kind.STATIC ? rawComparator : UTF8Type.instance;
}

// AFTER — raw string used; compact_value handled explicitly
public static AbstractType<?> columnDefinitionComparator(
        String kind, boolean isSuper,
        AbstractType<?> rawComparator, AbstractType<?> rawSubComparator) {
    if ("compact_value".equals(kind))
        return UTF8Type.instance;
    if (isSuper)
        return "regular".equals(kind) ? rawSubComparator : UTF8Type.instance;
    return "static".equals(kind) ? rawComparator : UTF8Type.instance;
}
```

---

## How to Spot
Legacy schema migration code that converts string-typed metadata into enums before passing to a helper method is vulnerable to this pattern: any string value that has no enum counterpart silently maps to a default or null, introducing an invisible lossy conversion. When reviewing such code, verify that the complete set of strings present in the legacy tables (including undocumented pseudo-kinds like `compact_value`) is handled before enum conversion, or pass the raw string to helpers that need the full fidelity.

---

## Test
No test added.
