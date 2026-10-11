# Bug 2551: CommitLog segments never recycled due to uninitialized position variable

**Commit**: 5914d42a7e · **Ticket**: CASSANDRA-3269

## Detection Rule
When a position variable is initialised to a sentinel value (`-1L`) outside a `try` block but only assigned inside it, and the assignment to `turnOn()` uses the sentinel rather than the real position, dirty CF tracking is broken and segments are never marked clean.

## Code Smell
A `long currentPosition = -1L` is declared before the `try` block; inside the block, `turnOn(cfm.cfId, (int) currentPosition)` is called before `currentPosition` is ever updated, passing `-1` as the log position. The `catch` block further guards truncation with `if (currentPosition != -1)`, preventing cleanup on first-write errors.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: uninitialised-position-variable
- severity: data-loss
- subsystem: commitlog-cdc

## Root Cause
`CommitLogSegment.write()` called `turnOn(cfm.cfId, (int) currentPosition)` with `currentPosition` still at its initial `-1L` value because `getContext()` was called after the loop. As a result, every CF's dirty position was recorded as `-1`, so no segment could ever be considered obsolete and old segments accumulated without being recycled. The fix moves `ReplayPosition cLogCtx = getContext()` before the `try` block and passes `cLogCtx.position` to `turnOn`.

## Example
```java
// BEFORE
long currentPosition = -1L;
try {
    ReplayPosition cLogCtx = getContext();
    for (ColumnFamily cf : ...) {
        turnOn(cfm.cfId, (int) currentPosition); // always -1
    }
    ...
} catch (IOException e) {
    if (currentPosition != -1)      // never true on first write
        logWriter.truncate(currentPosition);
}
```
```java
// AFTER
ReplayPosition cLogCtx = getContext();
try {
    for (ColumnFamily cf : ...) {
        turnOn(cfm.cfId, cLogCtx.position); // correct
    }
    ...
} catch (IOException e) {
    logWriter.truncate(cLogCtx.position);
}
```
