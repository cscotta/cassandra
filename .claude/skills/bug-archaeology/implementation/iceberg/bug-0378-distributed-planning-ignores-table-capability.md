# Bug 0378: Distributed scan planning enabled for tables that do not support it

**Commit**: 580e793ef220404236177935e6ee94d1ccfe4668 · **Ticket**: #15184

## Detection Rule

When a method returns a boolean capability flag by combining multiple conditions, verify that it first checks whether the object implementing that capability actually declares support for it — not just whether configuration permits it.

## Code Smell

`distributedPlanningEnabled()` returns `dataPlanningMode() != LOCAL || deletePlanningMode() != LOCAL` without first checking whether the `table` implements the interface that declares distributed planning support, allowing any table with non-LOCAL planning modes to be routed into `SparkDistributedDataScan` regardless of table type.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: capability-check-missing
- severity: silent-wrong-result
- subsystem: spark/scan-planning

## Root Cause

`distributedPlanningEnabled()` checked only Spark configuration (planning modes) and not whether the table itself supports distributed planning. The `SparkScanBuilder.newBatchScan()` previously guarded `SparkDistributedDataScan` with a `BaseTable` instanceof check, but after that guard was removed, any table with non-LOCAL planning mode — including custom table implementations that should use their own `newBatchScan()` — would be silently routed into the distributed scan path. The fix adds a `SupportsDistributedScanPlanning` interface check so that `RESTTable.allowDistributedPlanning()` can return `false` and opt out.

## Example

```java
// BEFORE: only checks config, not table capability
public boolean distributedPlanningEnabled() {
  return dataPlanningMode() != LOCAL || deletePlanningMode() != LOCAL;
}

private BatchScan newBatchScan() {
  if (table instanceof RequiresRemoteScanPlanning) {
    return table.newBatchScan();           // REST path
  } else if (table instanceof BaseTable && readConf.distributedPlanningEnabled()) {
    return new SparkDistributedDataScan(spark, table, readConf);
  } else {
    return table.newBatchScan();
  }
}
```

```java
// AFTER: table must declare support AND config must permit it
public boolean distributedPlanningEnabled() {
  return table instanceof SupportsDistributedScanPlanning distributed
      && distributed.allowDistributedPlanning()
      && (dataPlanningMode() != LOCAL || deletePlanningMode() != LOCAL);
}

private BatchScan newBatchScan() {
  if (readConf.distributedPlanningEnabled()) {
    return new SparkDistributedDataScan(spark, table, readConf);
  } else {
    return table.newBatchScan();
  }
}
```
