# Bug 2822: Read Repair Resolver Called Even When No Data Response Received

**Commit**: 71ccb7d60e · **Ticket**: CASSANDRA-1985

## Detection Rule
When a read repair callback's `get()` method calls `resolver.resolve()` unconditionally after a timeout wait, and `resolve()` throws an exception when no data response was received (only digest responses), the read repair silently fails or throws `DigestMismatchException` for a non-existent data discrepancy.

## Code Smell
`return resolver.resolve()` at the end of `RepairCallback.get()` with no guard — if only digest responses arrived and no data response, `resolve()` has no row data to return and may throw or return stale/wrong results.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: resolve-without-data-present-check
- severity: silent-wrong-result
- subsystem: repair

## Root Cause
`RepairCallback.get()` waited on a condition for all expected responses, then called `resolver.resolve()`. If the repair target never sent a data response (only a digest, or no response within timeout), `resolver.isDataPresent()` would return false. Calling `resolve()` without checking caused it to operate on an empty or incomplete response set, potentially throwing exceptions or returning wrong data. The fix adds a guard: `return resolver.isDataPresent() ? resolver.resolve() : null`. Additionally, the on-digest-mismatch path in `StorageProxy` was changed to schedule the repair callback's `get()` asynchronously after the RPC timeout, preventing blocking the read response path.

## Example
```java
// BEFORE: resolve() called unconditionally
public Row get() throws DigestMismatchException, IOException, TimeoutException {
    condition.await(DatabaseDescriptor.getRpcTimeout(), TimeUnit.MILLISECONDS);
    return resolver.resolve(); // throws if no data response arrived
}
```
```java
// AFTER: only resolve if data is present
public Row get() throws DigestMismatchException, IOException, TimeoutException {
    condition.await(DatabaseDescriptor.getRpcTimeout(), TimeUnit.MILLISECONDS);
    return resolver.isDataPresent() ? resolver.resolve() : null;
}
```
