# Bug 2517: Per-row secondary index updates skip the first column due to misplaced `else`

**Commit**: 632887c4c8 · **Ticket**: CASSANDRA-3441

## Detection Rule
When a `HashSet` is lazily initialized in an `if (set == null)` block followed by an `else if (set.add(...))` branch, the add is only executed on the second and later iterations; the first column is skipped because the `else` prevents `set.add()` from running immediately after initialization.

## Code Smell
`if (appliedRowLevelIndexes == null) ... else if (appliedRowLevelIndexes.add(index.getClass()))` — the `else` means: only call `applyIndexUpdates` if the set was already non-null, so the very first column processed for a per-row index is silently dropped.

## Tags
- scope: java
- category: logic-error
- code-pattern: else-after-lazy-init-skips-first
- severity: silent-wrong-result
- subsystem: sai-secondary-index

## Root Cause
The pattern `if (set == null) { set = new HashSet(); } else { if (set.add(x)) doWork(); }` ensures `doWork()` is never called for the element that triggers initialization. After creation the set is empty so `set.add(x)` for the first element would succeed, but the `else` guard prevents that branch from being reached. Every call site in `SecondaryIndexManager` that handled per-row indexes had this bug, meaning the first indexed column in any row update was never forwarded to the per-row index implementation.

## Example
```java
// BEFORE
if (appliedRowLevelIndexes == null)
    appliedRowLevelIndexes = new HashSet<>();
else
    if (appliedRowLevelIndexes.add(index.getClass()))
        ((PerRowSecondaryIndex)index).applyIndexUpdates(...); // skipped for first col
```
```java
// AFTER
if (appliedRowLevelIndexes == null)
    appliedRowLevelIndexes = new HashSet<>();

if (appliedRowLevelIndexes.add(index.getClass()))
    ((PerRowSecondaryIndex)index).applyIndexUpdates(...);
```
