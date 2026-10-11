# Bug 2572: RowRepairResolver NPE when ColumnFamily is null

**Commit**: c2b071e2 · **Ticket**: CASSANDRA-3192

## Detection Rule
When iterating over read-repair responses, calling a method on a `ColumnFamily` object without a null check first will throw `NullPointerException` if a replica returned an empty (null CF) response.

## Code Smell
`cf.getLiveColumnCount()` is called on a `cf` variable that may be `null` (representing an empty row from a replica), without a prior null guard.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: null-cf-dereference
- severity: crash
- subsystem: cql-query

## Root Cause
`RowRepairResolver` iterated over responses and called `cf.getLiveColumnCount()` to determine the maximum live column count across replicas (used to prevent "short reads"). However, a replica that had no data for the requested key returns a `null` `ColumnFamily`. Calling `.getLiveColumnCount()` on null threw an NPE, aborting the read even when data was available on other replicas.

## Example
```java
// BEFORE
int liveColumns = cf.getLiveColumnCount(); // NPE if cf == null
if (liveColumns > maxLiveColumns)
    maxLiveColumns = liveColumns;
```
```java
// AFTER
int liveColumns = cf == null ? 0 : cf.getLiveColumnCount();
if (liveColumns > maxLiveColumns)
    maxLiveColumns = liveColumns;
```
