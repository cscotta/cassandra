# Bug 0202: Scan-specific FileIO not passed to Spark 4.1 executor readers

**Commit**: be25b80f7fa06406343f894463b72ded5f383e9e · **Ticket**: #15448

## Detection Rule

When a component creates a scan object that carries specialised state (e.g. a REST plan-ID embedded in its FileIO), verify that the same FileIO instance — not `table.io()` — is forwarded to every downstream reader/executor that opens data files.

## Code Smell

`BaseReader` (and all its subclasses) called `EncryptingFileIO.combine(table().io(), ...)` directly, ignoring the `FileIO` instance that was associated with the scan; `SparkBatch` and `SparkMicroBatchStream` likewise called `table.io()` for block-locality resolution.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: stale-state-after-lifecycle
- severity: silent-wrong-result
- subsystem: spark/read-path

## Root Cause

Same root cause as the Spark 4.0 fix (bug-0201): when remote scan planning is active, `RESTTableScan.fileIO()` returns a specialised `FileIO` carrying a `rest-scan-plan-id`. The Spark 4.1 module had the identical copy of the code and was never updated. Without the plan-ID, executor-side reads cannot be correlated with the server-side pre-planned scan.

## Example

```java
// BEFORE — SparkBatch computed block locations from the table's default IO
private String[][] computePreferredLocations() {
    if (localityEnabled) {
        return SparkPlanningUtil.fetchBlockLocations(table.io(), taskGroups);
    }
}
```

```java
// AFTER — scan's FileIO is broadcast and used everywhere
private String[][] computePreferredLocations() {
    if (localityEnabled) {
        return SparkPlanningUtil.fetchBlockLocations(fileIO.get(), taskGroups);
    }
}
```
