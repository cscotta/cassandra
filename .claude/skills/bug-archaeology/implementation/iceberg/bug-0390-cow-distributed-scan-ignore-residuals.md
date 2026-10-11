# Bug 0390: CoW distributed scan ignores ignoreResiduals and uses wrong scan factory

**Commit**: 854e3b2c88c3390274a1da5a25f87419cd846d39 · **Ticket**: #15246

## Detection Rule

When a scan is configured with `ignoreResiduals()` and then serialized for distributed execution, verify that the residual-ignore flag is forwarded to every inner scan builder created during planning, including delete manifest readers that construct a separate scan from the same context.

## Tags

- scope: project-specific
- category: state-not-cleaned
- code-pattern: stale-state-after-lifecycle
- severity: silent-wrong-result
- subsystem: core/distributed-planning

## Code Smell

A distributed scan builder that reads `ignoreResiduals()` from its own context but does not call `builder.ignoreResiduals()` when constructing the inner `DeleteFileIndex.Builder` — and a `SparkScanBuilder.buildCopyOnWriteScan()` that calls `table.newBatchScan()` directly instead of the overridable `newBatchScan()` factory method, bypassing the distributed scan type.

## Root Cause

`BaseDistributedDataScan` forwarded `planExecutor()` to its inner manifest-group builder but omitted `ignoreResiduals()`. The `ReadDeleteManifest` serializable task also used `context.rowFilter()` unconditionally rather than substituting `alwaysTrue()` when `context.ignoreResiduals()` was set. Additionally, `buildCopyOnWriteScan()` bypassed the overridden factory by calling `table.newBatchScan()` instead of `newBatchScan()`.

## Example

```java
// BEFORE
// BaseDistributedDataScan — ignoreResiduals not forwarded
if (planExecutor() != null) {
    builder.planWith(planExecutor());
}
return builder.specsById(specs())...

// ReadDeleteManifest — residuals always applied
this.filter = context.rowFilter();

// SparkScanBuilder — bypasses factory override
BatchScan scan = table.newBatchScan()
    .useSnapshot(snapshot.snapshotId())
    .ignoreResiduals()...
```

```java
// AFTER
if (shouldIgnoreResiduals()) {
    builder.ignoreResiduals();
}
return builder.specsById(specs())...

// ReadDeleteManifest — honours flag
this.filter = context.ignoreResiduals() ? Expressions.alwaysTrue() : context.rowFilter();

// SparkScanBuilder — uses polymorphic factory
BatchScan scan = newBatchScan()
    .useSnapshot(snapshot.snapshotId())
    .ignoreResiduals()...
```
