# Bug 0151: Z-order rewrite silently corrupts results when table has a column named ICEZVALUE

**Commit**: 24cb0389034d7718308d85f16c65147de29547cf · **Ticket**: #15706

## Detection Rule

When a rewrite or compaction operation adds a synthetic internal column to a DataFrame, verify that the column name is checked against all existing table column names (case-insensitively when the session is case-insensitive) before proceeding.

## Code Smell

A hardcoded internal column name constant (e.g. `Z_COLUMN = "ICEZVALUE"`) is injected into a DataFrame without any prior check that the schema does not already contain a field with that name, creating a silent collision path.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: internal-name-collides-with-schema
- severity: silent-wrong-result
- subsystem: spark/actions/z-order-rewrite

## Root Cause

`SparkZOrderFileRewriteRunner` adds an intermediate column named `ICEZVALUE` to the DataFrame during Z-order computation. If the user's table already contains a column with that name, the synthetic column silently shadows or merges with the user column, producing incorrect Z-order values and therefore a mis-ordered output file. No validation was performed before building the column list.

## Example

```java
// BEFORE
List<String> validZOrderColNames = Lists.newArrayList();

for (String colName : inputZOrderColNames) {
  // no check that schema does not already contain Z_COLUMN
```

```java
// AFTER
Preconditions.checkArgument(
    caseSensitive
        ? schema.findField(Z_COLUMN) == null
        : schema.caseInsensitiveFindField(Z_COLUMN) == null,
    "Cannot zorder because the table has a column named '%s', which conflicts with Iceberg's internal Z-order column name",
    Z_COLUMN);

List<String> validZOrderColNames = Lists.newArrayList();
```
