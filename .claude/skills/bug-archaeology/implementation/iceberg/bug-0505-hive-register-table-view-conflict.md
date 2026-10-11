# Bug 0505: HiveCatalog registerTable silently overwrites existing view

**Commit**: bd96b79b4229f27c0aa4a0ed776a1493499cfd79 · **Ticket**: #15010

## Detection Rule

When a catalog implementation overrides table/view conflict checks in `doCommit()` or `doRefresh()` but does NOT override `registerTable()`, verify that `registerTable()` also performs the same existence checks before writing metadata.

## Code Smell

A `registerTable()` method inherited from a base class that calls `super.registerTable()` directly without first checking `viewExists()`, while sibling `create()` and `createOrReplaceTransaction()` methods in the same class do call `viewExists()`.

## Tags

- scope: project-specific
- category: missing-null-check
- code-pattern: missing-precondition-on-alternate-path
- severity: silent-wrong-result
- subsystem: hive-metastore catalog

## Root Cause

`HiveCatalog` detects table/view conflicts during `doRefresh()` rather than `doCommit()`, which is earlier than other catalogs. However, `registerTable()` bypasses `doRefresh()` entirely — it writes metadata directly — so the existing-view guard was never exercised. Calling `registerTable()` with an identifier that already named a Hive view would succeed silently, leaving the catalog in an inconsistent state where a table entry shadowed a view.

## Example

```java
// BEFORE: registerTable() not overridden in HiveCatalog,
// inherited base does no viewExists() check
public Table registerTable(TableIdentifier identifier, String metadataFileLocation) {
    // base class proceeds directly to writing table entry — no view check
    return super.registerTable(identifier, metadataFileLocation);
}
```

```java
// AFTER: HiveCatalog overrides registerTable() with explicit checks
@Override
public Table registerTable(TableIdentifier identifier, String metadataFileLocation) {
    if (tableExists(identifier)) {
        throw new AlreadyExistsException("Table already exists: %s", identifier);
    }
    if (viewExists(identifier)) {
        throw new AlreadyExistsException("View with same name already exists: %s", identifier);
    }
    return super.registerTable(identifier, metadataFileLocation);
}
```
