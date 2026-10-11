# Bug 0201: Scan-specific FileIO not passed to Spark executor readers

**Commit**: 66c087f2411ae09023cc5bb1b628f289dba012ad · **Ticket**: #15682

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

When remote scan planning (REST server-side planning) is used, `RESTTableScan.fileIO()` returns a specialised `FileIO` whose properties include a `rest-scan-plan-id` that the server uses to serve pre-planned file lists. The scan object was never threaded through to `SparkBatch`, `SparkInputPartition`, or `BaseReader`; those classes always fell back to `table.io()`, which lacks the plan-ID property, causing the server to be unable to associate file reads with the correct scan plan.

## Example

```java
// BEFORE — BaseReader always used the table's default IO
private Map<String, InputFile> inputFiles() {
    this.lazyInputFiles =
        EncryptingFileIO.combine(table().io(), table().encryption())
            .bulkDecrypt(...);
}
```

```java
// AFTER — caller passes the scan's FileIO; BaseReader stores and uses it
BaseReader(Table table, FileIO fileIO, ...) {
    this.fileIO = EncryptingFileIO.combine(fileIO, table().encryption());
    ...
}
private Map<String, InputFile> inputFiles() {
    this.lazyInputFiles = fileIO.bulkDecrypt(...);
}
```
