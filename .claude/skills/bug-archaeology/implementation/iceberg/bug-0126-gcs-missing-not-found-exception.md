# Bug 0126: GCS read of nonexistent file throws generic IOException instead of NotFoundException

**Commit**: fd21d667fb51433f08df8e538054a1461822b328 · **Ticket**: #15734

## Detection Rule

When a storage-layer input stream catches an `IOException` wrapping a provider-specific 404 status, verify that it re-throws the framework's `NotFoundException` so that callers (e.g. `TableOperations`) can distinguish "file does not exist" from transient I/O errors and skip unnecessary retries.

## Code Smell

A read method on a cloud-storage input stream that lets a raw `IOException` (or a provider SDK exception such as `StorageException`) propagate without inspecting the HTTP status code, where the framework defines a dedicated `NotFoundException` for the "object not found" case.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: missing-404-to-not-found-translation
- severity: performance
- subsystem: gcp / gcs / GCSInputStream, GcsInputStreamWrapper

## Root Cause

`GCSInputStream` and `GcsInputStreamWrapper` propagated the raw `IOException` from the underlying Google Cloud Storage channel without checking whether the cause was a `StorageException` with HTTP code 404. The Iceberg `TableOperations` contract relies on receiving `NotFoundException` to know that no retry is warranted; receiving a plain `IOException` causes unnecessary retry attempts and obscures the real problem. The fix adds a `GCSExceptionUtil.throwNotFoundIfNotPresent` helper that is called on every read path before re-throwing the original exception.

## Example

```java
// BEFORE (GCSInputStream.read)
channel.read(singleByteBuffer);
```

```java
// AFTER (GCSInputStream.read)
try {
  channel.read(singleByteBuffer);
} catch (IOException e) {
  GCSExceptionUtil.throwNotFoundIfNotPresent(e, blobId);
  throw e;
}

// GCSExceptionUtil.throwNotFoundIfNotPresent
static void throwNotFoundIfNotPresent(IOException ioException, BlobId blobId) {
  if (ioException.getCause() instanceof StorageException storageException
      && storageException.getCode() == 404) {
    throw new NotFoundException(ioException, "Location does not exist: %s", blobId.toGsUtilUri());
  }
}
```
