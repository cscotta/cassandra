# Bug 0279: Bulk delete of metadata files throws uncaught exception

**Commit**: 6b2e8045398918d3392518d005a04d0e9c9219ab · **Ticket**: #15464

## Detection Rule

When a `SupportsBulkOperations` path is taken to delete files, verify the call is wrapped with the same error-suppression (try/catch or `suppressFailureWhenFinished`) that is applied to the non-bulk path; asymmetric error handling silently converts a best-effort operation into a hard failure.

## Code Smell

An `if (io instanceof SupportsBulkOperations) { bulkIO.deleteFiles(...); }` block with no surrounding try/catch alongside an `else` branch that uses `Tasks.foreach(...).suppressFailureWhenFinished()` — errors from bulk deletion propagate while errors from individual deletion are swallowed.

## Root Cause

`CatalogUtil.deleteRemovedMetadataFiles()` had two code paths: when `FileIO` implemented `SupportsBulkOperations` it called `deleteFiles()` bare, without catching exceptions; the non-bulk path used `Tasks.foreach().suppressFailureWhenFinished()` so failures were only logged. A bulk-delete failure would therefore abort the caller and surface as an unexpected exception, whereas the same failure through the non-bulk path would be silently ignored. The fix replaces the inline split with a call to the shared `deleteFiles(io, paths, type, true)` helper which already handles the try/catch for bulk operations.

## Example

```java
// BEFORE
if (io instanceof SupportsBulkOperations) {
    ((SupportsBulkOperations) io)
        .deleteFiles(
            Iterables.transform(removedPreviousMetadataFiles,
                                TableMetadata.MetadataLogEntry::file));
    // no try/catch — exceptions propagate to caller
} else {
    Tasks.foreach(removedPreviousMetadataFiles)
        .noRetry()
        .suppressFailureWhenFinished()
        .onFailure((f, exc) -> LOG.warn("Delete failed for previous metadata file: {}", f, exc))
        .run(f -> io.deleteFile(f.file()));
}
```

```java
// AFTER
deleteFiles(
    io,
    removedPreviousMetadataFiles.stream()
        .map(TableMetadata.MetadataLogEntry::file)
        .collect(Collectors.toSet()),
    "metadata",
    true);   // shared helper suppresses failures for both bulk and non-bulk paths
```
