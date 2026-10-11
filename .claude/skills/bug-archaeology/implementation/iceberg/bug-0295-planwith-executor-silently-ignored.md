# Bug 0295: planWith executor silently ignored when scanning ManifestGroup entries

**Commit**: 86e9ebbffde79ae270c3d57d621e509f9142b27c · **Ticket**: #15426

## Detection Rule

When a builder or fluent API accepts an `ExecutorService` via a `planWith()` / `withExecutor()` setter and a caller passes one, verify that the terminal method actually uses `ParallelIterable` (or equivalent) rather than falling back to a sequential `concat()`.

## Code Smell

A class stores `executorService` from a `planWith()` call, but the `entries()` terminal method always returns `CloseableIterable.concat(...)` regardless of whether `executorService` is non-null; the stored executor is only used in an unrelated code path.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: option-set-but-not-used
- severity: performance
- subsystem: core/manifest-group

## Root Cause

`ManifestGroup.entries()` delegated to `entries((manifest, entries) -> entries)` and then wrapped the result with `CloseableIterable.concat()`, which is always sequential. The `executorService` field was set by `planWith()` and used in `planFiles()` / `planDeleteFiles()`, but `entries()` never consulted it. `FindFiles` called `planWith(executorService)` but the returned iterable was never parallel. The fix introduces an overloaded `entries(Function)` that checks `executorService != null` and returns a `ParallelIterable` instead of `concat`.

## Example

```java
// BEFORE: executorService is stored but entries() always uses concat
public CloseableIterable<ManifestEntry<DataFile>> entries() {
    return CloseableIterable.concat(entries((manifest, entries) -> entries));
}

// FindFiles – planWith is called but has no effect on entries()
CloseableIterable<ManifestEntry<DataFile>> entries =
    new ManifestGroup(ops.io(), snapshot.dataManifests(ops.io()))
        .planWith(executorService)
        .entries();  // always sequential
```

```java
// AFTER: new overload checks executorService and uses ParallelIterable
public <T> CloseableIterable<T> entries(
    Function<CloseableIterable<ManifestEntry<DataFile>>, CloseableIterable<T>> transform) {
  Iterable<CloseableIterable<T>> iterables = entries((manifest, e) -> transform.apply(e));
  if (executorService != null) {
    return new ParallelIterable<>(iterables, executorService);
  } else {
    return CloseableIterable.concat(iterables);
  }
}
```
