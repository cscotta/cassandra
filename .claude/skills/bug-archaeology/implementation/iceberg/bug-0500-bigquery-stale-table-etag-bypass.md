# Bug 0500: BigQuery commit uses stale ETag from a second table load, bypassing conflict detection

**Commit**: 38cc88136684a57b61be4ae0d2c1886eff742a28 · **Ticket**: n/a

## Detection Rule

When a metastore operations class loads a remote resource in `doRefresh()` and then loads the same resource again in `doCommit()` to obtain an ETag or version for conflict detection, verify that the second load is not defeating optimistic-locking — a concurrent writer may have updated the resource between the two loads, letting the commit proceed against the wrong base state.

## Code Smell

A `doCommit()` method that calls `client.load(ref)` when the same ref was already loaded moments earlier in `doRefresh()`, using the freshly loaded version for ETag/conflict checks while discarding the version captured during refresh.

## Tags

- scope: distributed-systems
- category: race-condition
- code-pattern: stale-state-after-lifecycle
- severity: silent-wrong-result
- subsystem: bigquery / metastore-operations

## Root Cause

`doRefresh()` loaded the BigQuery table and recorded `currentMetadataLocation()`. `updateTable()` then issued a second `client.load()` call to obtain the ETag for the `If-Match` update. Between those two loads, a concurrent writer could have committed and advanced both the metadata location and the ETag. The second load would capture the concurrent writer's ETag, so the `client.update()` call would succeed against a base state the current transaction never read, silently committing on top of unseen changes. The fix stores the table object from `doRefresh()` in a field and reuses it in `updateTable()`, ensuring the ETag presented to the API matches exactly what was read when the transaction started.

## Example

```java
// BEFORE: second load in updateTable() races with concurrent writers
private void updateTable(String oldMetadataLocation, String newMetadataLocation, TableMetadata metadata) {
    Table table = client.load(tableReference);   // second load — ETag may already be stale
    ExternalCatalogTableOptions options = table.getExternalCatalogTableOptions();
    // metadata-location check could also pass if concurrent write happened after first load
    client.update(tableReference, table);
}
```

```java
// AFTER: reuse table captured during doRefresh()
private volatile Table metastoreTable;   // set in doRefresh(), cleared after commit

public void doRefresh() {
    this.metastoreTable = client.load(tableReference);   // single load
    metadataLocation = loadMetadataLocationOrThrow(metastoreTable.getExternalCatalogTableOptions());
}

private void updateTable(String newMetadataLocation, TableMetadata metadata) {
    Preconditions.checkState(metastoreTable != null, ...);
    // ETag from the same snapshot the transaction was based on
    client.update(tableReference, metastoreTable);
    this.metastoreTable = null;
}
```
