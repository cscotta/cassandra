# Bug 0581: View query body silently not updated in HMS when replacing a view

**Commit**: 59280a106ca5f378edfd47d3576c4d10011e5e80 · **Ticket**: #14831

## Detection Rule

When a replace/update operation mutates a logical entity's definition AND that entity is mirrored in an external store, verify that every field in the external store's representation is re-written, not just the metadata properties.

## Code Smell

A method that updates an external record accepts an entity object but lacks a parameter carrying the entity's core definition text; the update method only touches property maps, leaving the definition field stale.

## Tags

- scope: project-specific
- category: state-not-cleaned
- code-pattern: stale-state-after-lifecycle
- severity: silent-wrong-result
- subsystem: hive-metastore / view-operations

## Root Cause

`HMSTablePropertyHelper.setViewProperties()` was responsible for updating the HMS table record when a view was replaced, but it only updated the Iceberg metadata location and schema parameters. The HMS-native `viewOriginalText` and `viewExpandedText` fields—used by Hive to display and execute the view—were never overwritten after the initial creation. Any `replaceView` call therefore left HMS with the original SQL query, causing HMS clients to see stale view definitions.

## Example

```java
// BEFORE — sqlQuery never passed; viewOriginalText/viewExpandedText stay stale
public static void setViewProperties(
    Table tbl,
    ViewMetadata metadata,
    Set<String> obsoleteProps,
    long maxHiveTablePropertySize,
    String currentLocation) {
  // ... updates parameters map only
  tbl.setParameters(parameters);
}
```

```java
// AFTER — SQL passed and applied to the HMS table record
public static void setViewProperties(
    Table tbl,
    ViewMetadata metadata,
    Set<String> obsoleteProps,
    long maxHiveTablePropertySize,
    String currentLocation,
    String sqlQuery) {
  // ... updates parameters map
  tbl.setParameters(parameters);
  if (sqlQuery != null) {
    tbl.setViewExpandedText(sqlQuery);
    tbl.setViewOriginalText(sqlQuery);
  }
}
```
