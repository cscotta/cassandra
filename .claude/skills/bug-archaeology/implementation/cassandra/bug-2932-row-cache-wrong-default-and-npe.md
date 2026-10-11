# Bug 2932: Row cache default fraction wrong, and NPE on super-column read when cache miss

**Commit**: 70e1ee304c · **Ticket**: CASSANDRA-689

## Detection Rule
If the default row-cache fraction is non-zero, column families that did not explicitly configure a cache will have a small cache enabled unintentionally; additionally, if `getCachedRow()` can return null, dereferencing the result without a null check causes an NPE.

## Code Smell
`getRowsCachedFraction()` falls back to `0.01` instead of `0`; and in the super-column read path, `cf` and `sc` returned from cache methods are dereferenced without null checks.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: wrong-default-and-missing-null-check
- severity: crash
- subsystem: storage-engine

## Root Cause
Two related bugs: (1) `DatabaseDescriptor.getRowsCachedFraction()` returned `0.01` as the default when no explicit cache fraction was configured, silently enabling a small row cache for all column families. Changing the default to `0` makes caching opt-in. (2) The super-column read path called `getCachedRow()` and then `cf.getColumn(superColumnName)` without checking whether `cf` or the returned `sc` was null, causing NPEs when a cache miss or absent super-column occurred.

## Example
```java
// BEFORE
return v == null ? 0.01 : v;  // unintentional default cache for all CFs

// and in read path:
cf = getCachedRow(filter);
sc = (SuperColumn)cf.getColumn(filter.path.superColumnName); // NPE if cf==null
```
```java
// AFTER
return v == null ? 0 : v;  // caching disabled by default

cf = getCachedRow(filter);
if (cf == null) return null;
sc = (SuperColumn)cf.getColumn(filter.path.superColumnName);
if (sc == null) return null;
```
