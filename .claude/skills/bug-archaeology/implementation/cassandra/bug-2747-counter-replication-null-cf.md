# Bug 2747: Counter Replication NPE on Row with Null Column Family

**Commit**: 4618b41ea9 · **Ticket**: CASSANDRA-2289

## Detection Rule
When reading a row to replicate counter values, check both `row == null` and `row.cf == null` before accessing the row's column family metadata.

## Code Smell
`CounterMutation.readLocalValues` guards only against a null `row` but immediately dereferences `row.cf.metadata()` without checking whether `row.cf` itself is null (possible when the row key exists in the index but has no column family data yet).

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: partial-null-guard
- severity: crash
- subsystem: storage-engine

## Root Cause
A read command may return a `Row` object with a non-null key but a null `cf` field when no columns match the request. The counter replication path checked only `row == null`, so it NPE'd when `row.cf` was null. The fix extends the condition to `row == null || row.cf == null`.

## Example
```java
// BEFORE
if (null == row)
    continue;
AbstractType defaultValidator = row.cf.metadata().getDefaultValidator();
```
```java
// AFTER
if (row == null || row.cf == null)
    continue;
AbstractType defaultValidator = row.cf.metadata().getDefaultValidator();
```
