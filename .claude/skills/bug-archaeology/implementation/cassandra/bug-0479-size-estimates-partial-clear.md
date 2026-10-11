# Bug 0479: Legacy size_estimates Not Fully Cleared on refreshsizeestimates

**Commit**: 01103111ae · **Ticket**: CASSANDRA-15776
**Subsystem**: `src/java/org/apache/cassandra/db/SystemKeyspace.java`, `src/java/org/apache/cassandra/service/StorageService.java`
**Category**: State not cleaned up

---

## Root Cause

`StorageService#cleanupSizeEstimates` iterated the entries in `system.table_estimates` and deleted only rows for keyspaces or tables that no longer exist in the live schema. If `table_estimates` had entries that `getTablesWithSizeEstimates` did not return (e.g. because the query was wrong or data was only in `size_estimates`), stale rows survived. The method also performed per-keyspace and per-table DELETE queries rather than a bulk truncation. The fix replaces the selective-delete logic with `TRUNCATE TABLE` on both `LegacySizeEstimates` and `TableEstimates`, which are repopulated immediately after by the standard estimates refresh, ensuring no orphaned rows survive across keyspace or table drops.

### Before / After

```java
// BEFORE
SetMultimap<String, String> sizeEstimates = SystemKeyspace.getTablesWithSizeEstimates();
for (Entry<String, Collection<String>> tablesByKeyspace : sizeEstimates.asMap().entrySet()) {
    if (!Schema.instance.getKeyspaces().contains(keyspace))
        SystemKeyspace.clearEstimates(keyspace);
    else {
        for (String table : tablesByKeyspace.getValue())
            if (Schema.instance.getTableMetadataRef(keyspace, table) == null)
                SystemKeyspace.clearEstimates(keyspace, table);
    }
}
```

```java
// AFTER
SystemKeyspace.clearAllEstimates(); // TRUNCATEs both size_estimates and table_estimates
```

---

## How to Spot

Selective-delete cleanup loops that filter based on live schema state can miss orphaned rows when the schema query and the estimates tables are not perfectly in sync. When the intent is "clear everything and repopulate", a TRUNCATE is more correct and simpler than a scan-and-delete loop. Review any cleanup method that reads from one system table to decide what to delete in another — if the refresh that follows is unconditional, the cleanup should be too.

---

## Test

`TableEstimatesTest#refreshTableEstimatesClearsInvalidEntries` and `refreshSizeEstimatesClearsInvalidEntries` insert bogus entries for nonexistent keyspaces/tables, call `nodetool refreshsizeestimates`, and assert the bogus rows are gone.
