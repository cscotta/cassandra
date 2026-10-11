# Bug 2655: CommitLogSegment.dirtyString NPE when column family is deleted — inverted null check

**Commit**: 0f4dd380e7 · **Ticket**: n/a

## Detection Rule
When looking up CF metadata by ID in debug logging code, guard the result with a null check before dereferencing, because the CF may have been dropped; the null check condition must be `m == null` (use the fallback) not `m != null` (use the fallback when non-null).

## Code Smell
`CommitLogSegment.dirtyString()` calls `DatabaseDescriptor.getCFMetaData(cfId).cfName` without any null guard, throwing NPE if the column family was dropped while its segment is still dirty; additionally the fixed code had the ternary condition inverted (`m == null ? m.cfName : "<deleted>"`).

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: null-deref-in-debug-logging
- severity: crash
- subsystem: commitlog-cdc

## Root Cause
`CommitLogSegment.dirtyString()` logged dirty column families by fetching `DatabaseDescriptor.getCFMetaData(cfId)` and accessing `.cfName` directly. If a column family was dropped between the write to the commit log and the debug logging call, `getCFMetaData()` returns `null`, causing a `NullPointerException`. The fix assigns to a local variable `m` and substitutes `"<deleted>"` when `m` is null. (Note: the patch as committed has the condition inverted — `m == null ? m.cfName : "<deleted>"` would still NPE; the intent was `m == null ? "<deleted>" : m.cfName`.)

## Example
```java
// BEFORE
sb.append(DatabaseDescriptor.getCFMetaData(cfId).cfName)  // NPE if CF dropped

// AFTER (intended)
CFMetaData m = DatabaseDescriptor.getCFMetaData(cfId);
sb.append(m == null ? "<deleted>" : m.cfName)
```
