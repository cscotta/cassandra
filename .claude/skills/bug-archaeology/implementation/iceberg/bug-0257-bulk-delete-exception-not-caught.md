# Bug 0257: BulkDeletionFailureException uncaught in CatalogUtil bulk delete path

**Commit**: d1bf56cc408211fbf1bf67eea277d9a0846b7108 · **Ticket**: #15501

## Detection Rule

When calling a method that can throw a specific checked-or-unchecked exception subclass, verify that the catch block names that subclass explicitly and does not rely solely on a broader `RuntimeException` catch that would miss it if the subclass does not extend `RuntimeException`.

## Code Smell

A `try { bulkIO.deleteFiles(...) } catch (RuntimeException e)` block in a bulk-delete helper where `BulkDeletionFailureException` is a distinct exception type that carries a partial-failure count; the catch clause swallows `RuntimeException` but cannot reach a `BulkDeletionFailureException` that escapes through a different inheritance path.

## Tags

- scope: java
- category: silent-skip
- code-pattern: wrong-exception-type-caught
- severity: crash
- subsystem: core/CatalogUtil

## Root Cause

`CatalogUtil.deleteFiles` caught only `RuntimeException` when delegating to `SupportsBulkOperations.deleteFiles()`. `BulkDeletionFailureException`, which signals a partial bulk-deletion failure with a count of un-deleted objects, was not handled inside this method, allowing it to propagate uncaught and abort callers that expected deletion errors to be suppressed. The fix adds an explicit `catch (BulkDeletionFailureException e)` clause before the `RuntimeException` catch and logs the partial-failure count.

## Example

```java
// BEFORE
if (io instanceof SupportsBulkOperations) {
    try {
        SupportsBulkOperations bulkIO = (SupportsBulkOperations) io;
        bulkIO.deleteFiles(files);
    } catch (RuntimeException e) {
        LOG.warn("Failed to bulk delete {} files", type, e);
    }
}
```

```java
// AFTER
if (io instanceof SupportsBulkOperations bulkIO) {
    try {
        bulkIO.deleteFiles(files);
    } catch (BulkDeletionFailureException e) {
        LOG.warn("Failed to bulk delete {} {} files", e.numberFailedObjects(), type, e);
    } catch (RuntimeException e) {
        LOG.warn("Failed to bulk delete {} files", type, e);
    }
}
```
