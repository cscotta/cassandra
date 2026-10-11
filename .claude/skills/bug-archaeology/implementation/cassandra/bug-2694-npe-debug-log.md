# Bug 2694: NPE in debug log from using column object after removal

**Commit**: 62cd28dfef · **Ticket**: n/a

## Detection Rule
When constructing a debug log message inside a loop that mutates an object and then removes it from a collection, use the loop variable (the key/name) rather than a field on the already-referenced object that may reflect the post-removal state.

## Code Smell
`cf.getComparator().getString(oldColumn.name())` used in a log statement after the code has already determined `oldColumn` is obsolete; the correct variable is `name`, which is the loop's key.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: stale-reference-in-log
- severity: crash
- subsystem: sai-secondary-index

## Root Cause
The debug log inside `Table.apply()` called `oldColumn.name()` to get the column name for the log message. However, at that point `oldColumn` could be null or stale relative to the loop's `name` variable, causing an NPE when debug logging was enabled. The fix uses `name` (the map key, already in scope) instead of `oldColumn.name()`.

## Example
```java
// BEFORE
logger.debug("skipping index update for obsolete mutation of "
    + cf.getComparator().getString(oldColumn.name()));
```
```java
// AFTER
logger.debug("skipping index update for obsolete mutation of "
    + cf.getComparator().getString(name));
```
