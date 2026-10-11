# Bug 0088: ADLS missing file throws generic RuntimeException instead of NotFoundException

**Commit**: 8808f7761637415f933b97c386f5646f3356896f · **Ticket**: #15806

## Detection Rule

When a storage I/O operation can fail because the target resource does not exist, verify that the caught exception is inspected for "not found" error codes and rethrown as the project's canonical `NotFoundException` before propagating the original exception.

## Code Smell

A `catch (RuntimeException e)` block that immediately logs and rethrows `e` without inspecting whether the underlying storage exception carries a "blob not found" or "path not found" error code. Callers (e.g. `TableOperations`) receive an opaque `RuntimeException` instead of a typed signal they can use to skip retries.

## Tags

- scope: project-specific
- category: wrong-serialization
- code-pattern: missing-error-translation
- severity: crash
- subsystem: azure/adls-file-io

## Root Cause

`ADLSInputStream.openStream` caught `RuntimeException` from the Azure SDK but did not examine the error code before rethrowing. Both `BlobStorageException` (with `BlobErrorCode.BLOB_NOT_FOUND`) and `DataLakeStorageException` (with error code `"PathNotFound"`) indicate a missing resource, but neither was translated to `NotFoundException`. As a result, `TableOperations` could not distinguish a non-existent file from a transient failure and would unnecessarily retry or surface a confusing error.

## Example

```java
// BEFORE
try {
    return fileClient.openInputStream(getInputOptions(range));
} catch (RuntimeException e) {
    LOG.error("Failed to open input stream for file {}", fileClient.getFilePath(), e);
    throw e;
}
```

```java
// AFTER
try {
    return fileClient.openInputStream(getInputOptions(range));
} catch (RuntimeException e) {
    throwNotFoundIfNotPresent(e, location); // translates BlobNotFound / PathNotFound
    LOG.error("Failed to open input stream for file {}", fileClient.getFilePath(), e);
    throw e;
}
```
