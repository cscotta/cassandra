# Bug 0582: CloseableIterable from planFiles() never closed in CatalogHandlers

**Commit**: 2a006ba8d0f9b668efcfbc6c523c87ceddf35b94 · **Ticket**: #14891

## Detection Rule

When a method returns a `CloseableIterable` (or any `Closeable`), verify that every call site that iterates it wraps the iterable in a try-with-resources block, not a plain for-each or variable assignment.

## Code Smell

A local variable is assigned the result of `scan.planFiles()` (typed as `Iterable`, discarding the `Closeable`), then iterated with for-each and `Iterables.partition()`, with no `close()` call anywhere in the method.

## Tags

- scope: universal
- category: resource-leak
- code-pattern: close-not-on-all-paths
- severity: performance
- subsystem: rest-catalog / scan-planning

## Root Cause

`CatalogHandlers.planFilesFor()` called `scan.planFiles()` and stored the result as a plain `Iterable<FileScanTask>`, losing the `CloseableIterable` reference. The underlying scan resources (thread pools, open file handles, network connections) were never released because `close()` was never called. The fix wraps the call in try-with-resources using the `CloseableIterable` type.

## Example

```java
// BEFORE — Iterable loses Closeable; no close() called
Iterable<FileScanTask> planTasks = scan.planFiles();
// ... iteration ...
return Pair.of(initialFileScanTasks, firstPlanTaskKey);
```

```java
// AFTER — try-with-resources ensures close() on all paths
try (CloseableIterable<FileScanTask> planTasks = scan.planFiles()) {
  // ... iteration ...
  return Pair.of(initialFileScanTasks, firstPlanTaskKey);
} catch (IOException e) {
  throw new RuntimeException(e);
}
```
