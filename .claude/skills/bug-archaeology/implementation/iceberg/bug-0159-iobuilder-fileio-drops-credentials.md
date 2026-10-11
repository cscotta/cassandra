# Bug 0159: ioBuilder-created FileIO silently drops vended storage credentials

**Commit**: 5dff6f6d778f1a944d87ac5029e56b953d24c3c5 · **Ticket**: #15752

## Detection Rule

When a factory/builder pattern has two code paths that both produce an object of the same interface, verify that any post-creation setup steps (e.g. injecting credentials, setting configuration) are applied on ALL paths, not just one.

## Code Smell

A method has an `if (builder != null) { return builder.apply(...); } else { ... }` branch where the else branch calls a utility that handles post-creation setup, but the if branch returns the object directly without that setup.

## Tags

- scope: java
- category: silent-skip
- code-pattern: divergent-code-paths-skip-setup
- severity: silent-wrong-result
- subsystem: core/rest/RESTSessionCatalog

## Root Cause

`RESTSessionCatalog.newFileIO()` has two paths: when `ioBuilder` is set it calls `ioBuilder.apply(context, properties)` and returns immediately; when it is null it delegates to `CatalogUtil.loadFileIO()` which calls `setCredentials()` on `SupportsStorageCredentials` implementations. The `ioBuilder` path skipped that post-creation `setCredentials()` call, so any vended storage credentials were silently discarded, leaving the FileIO unable to access the remote storage.

## Example

```java
// BEFORE
private FileIO newFileIO(..., List<Credential> storageCredentials) {
    if (null != ioBuilder) {
        return ioBuilder.apply(context, properties); // credentials silently dropped
    } else {
        ...
        return CatalogUtil.loadFileIO(ioImpl, properties, conf); // passes credentials
    }
}
```

```java
// AFTER
private FileIO newFileIO(..., List<Credential> storageCredentials) {
    if (null != ioBuilder) {
        FileIO fileIO = ioBuilder.apply(context, properties);
        if (!storageCredentials.isEmpty()
            && fileIO instanceof SupportsStorageCredentials ioWithCredentials) {
            ioWithCredentials.setCredentials(
                storageCredentials.stream()
                    .map(c -> StorageCredential.create(c.prefix(), c.config()))
                    .collect(Collectors.toList()));
        }
        return fileIO;
    } else {
        ...
    }
}
```
