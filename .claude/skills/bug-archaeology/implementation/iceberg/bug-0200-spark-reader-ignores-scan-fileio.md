# Bug 0200: Spark readers use table's default FileIO instead of scan-vended FileIO with credentials

**Commit**: 0c46639e0e2e60748d6823d3620bd70838151fa9 · **Ticket**: #15683

## Detection Rule

When a scan produces a specialized `FileIO` instance (e.g., one pre-initialized with scan-scoped storage credentials or a plan ID), verify that this FileIO is threaded through to every reader and broadcast variable that opens files, rather than falling back to `table.io()` which lacks those scan-specific properties.

## Code Smell

`EncryptingFileIO.combine(table().io(), table().encryption())` or `SparkPlanningUtil.fetchBlockLocations(table.io(), ...)` inside a reader or batch class that also accepts a `Scan` object; `table.io()` never carries scan-specific credentials or plan IDs that the scan-level `FileIO` holds.

## Tags

- scope: project-specific
- category: wrong-constant
- code-pattern: stale-io-reference
- severity: silent-wrong-result
- subsystem: spark/source

## Root Cause

`SparkScan`, `SparkBatch`, `SparkMicroBatchStream`, and all row/batch reader classes wired file I/O through `table.io()`, which is the table-level `FileIO` initialized at catalog load time. When a REST catalog returns a scan-scoped `FileIO` carrying plan-specific credentials or a `REST_SCAN_PLAN_ID` property, readers ignored it, so remote-scan file access would silently use the wrong (or unauthenticated) credentials. The fix adds a `Supplier<FileIO>` field propagated from the scan through `SparkBatch`, broadcast as `fileIOBroadcast`, and unpacked in `SparkInputPartition.io()`, then used in every reader instead of `table.io()`.

## Example

```java
// BEFORE — readers always use the table-level IO, ignoring scan credentials
private Map<String, InputFile> inputFiles() {
    this.lazyInputFiles =
        EncryptingFileIO.combine(table().io(), table().encryption())
            .bulkDecrypt(...);
}
```

```java
// AFTER — scan-specific FileIO (with credentials/planId) is used
BaseReader(Table table, FileIO fileIO, ...) {
    this.fileIO = EncryptingFileIO.combine(fileIO, table().encryption());
    ...
}

private Map<String, InputFile> inputFiles() {
    this.lazyInputFiles = fileIO.bulkDecrypt(...);
}
```
