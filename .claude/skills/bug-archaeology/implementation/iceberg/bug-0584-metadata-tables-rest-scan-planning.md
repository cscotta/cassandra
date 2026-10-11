# Bug 0584: Metadata tables returned as RESTTable breaking remote scan planning path

**Commit**: 05998edb8808ae21739072cca17b621925a2081a · **Ticket**: #14881

## Detection Rule

When a conditional guard selects between two implementations by calling a factory method and checking its return value, verify that the guard condition is derived from the input intent (e.g., request type), not from the factory's output, so inputs that should never reach the factory are excluded before the call.

## Code Smell

A null-check on the result of `restTableForScanPlanning(...)` is used to decide whether to use the REST table path, but the factory was already called unconditionally—including for metadata-table lookups where it should never be invoked.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: sentinel-not-handled
- severity: crash
- subsystem: rest-catalog / metadata-tables

## Root Cause

`RESTSessionCatalog.loadTable()` called `restTableForScanPlanning()` for every table load, including loads of metadata tables (e.g., `snapshots`, `manifests`). Metadata tables do not have manifest files accessible to the REST client, so using a `RESTTable` for them caused failures. The fix adds a prior check on `metadataType == null` so the REST scan-planning path is only entered for non-metadata tables, and `restTableForScanPlanning` is never called for metadata tables at all.

## Example

```java
// BEFORE — factory called for all table types; null check on result is insufficient guard
RESTTable restTable = restTableForScanPlanning(ops, finalIdentifier, tableClient);
if (restTable != null) {
  return restTable;
}
```

```java
// AFTER — metadataType gates the call; metadata tables bypass REST scan planning entirely
if (metadataType == null) {
  RESTTable restTable = restTableForScanPlanning(ops, finalIdentifier, tableClient);
  if (restTable != null) {
    return restTable;
  }
}
```
