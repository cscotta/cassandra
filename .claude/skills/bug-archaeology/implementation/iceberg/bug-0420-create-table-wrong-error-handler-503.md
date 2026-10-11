# Bug 0420: CREATE transactions use wrong error handler, triggering data cleanup on 503

**Commit**: 63d4084722824054b3460b8600f5cee9ab9a7248 · **Ticket**: #15051

## Detection Rule

When a table operation selects an error handler, verify that CREATE-table commit paths use a handler that treats 5xx responses as `CommitStateUnknown` rather than a recoverable or cleanable error; using the generic `tableErrorHandler` (which does not wrap 5xx as `CommitStateUnknownException`) on a CREATE path allows callers to clean up staged files after a response whose outcome is actually unknown.

## Code Smell

`RESTTableOperations` has a `switch` on commit type; in the `CREATE` branch it calls `ErrorHandlers.tableErrorHandler()` with a comment saying "throws NoSuchTableException", which is the handler designed for read-side lookups, not for commit-path 5xx uncertainty. The mismatch is visible because all other commit paths (`REPLACE`, commit handlers) use `ErrorHandlers.tableCommitHandler()` or its equivalent.

## Tags

- scope: project-specific
- category: wrong-constant
- code-pattern: wrong-error-handler-for-commit-path
- severity: data-loss
- subsystem: core/rest-catalog

## Root Cause

`RESTTableOperations` assigned `ErrorHandlers.tableErrorHandler()` for the `CREATE` commit type. `TableErrorHandler` does not handle 503 specially — it falls through to `DefaultErrorHandler`, which throws a plain `ServiceFailureException`. Because `CommitStateUnknownException` is never thrown, the surrounding transaction logic treats the error as a clean failure and proceeds to delete the staged data files, even though the server may have already committed them. The fix introduces a dedicated `CreateTableErrorHandler` that extends `CommitErrorHandler` (which maps 500/502/503/504 to `CommitStateUnknownException`) and overrides only the 404/409 cases relevant to table creation.

## Example

```java
// BEFORE — CREATE uses tableErrorHandler, which doesn't protect against data cleanup on 503
case CREATE:
    requirements = UpdateRequirements.forCreateTable(updates);
    errorHandler = ErrorHandlers.tableErrorHandler(); // no CommitStateUnknown for 5xx
    break;
```

```java
// AFTER — CREATE uses createTableErrorHandler, which wraps 5xx as CommitStateUnknownException
case CREATE:
    requirements = UpdateRequirements.forCreateTable(updates);
    errorHandler = ErrorHandlers.createTableErrorHandler(); // 503 → CommitStateUnknown
    break;
```
