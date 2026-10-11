# Bug 2686: Cached key position used for GT (greater-than) operator

**Commit**: d2c98b024d · **Ticket**: CASSANDRA-2633

## Detection Rule
When looking up a key's file position from the key cache, only return the cached position for equality (`EQ`) or greater-or-equal (`GE`) operators; for strictly greater-than (`GT`) the cached position is for the key itself, not the first key after it.

## Code Smell
Unconditionally consulting the key cache for any operator including `GT`, then returning the cached file position, which lands exactly on the sought key rather than the next key, causing the row to be incorrectly included in a `> key` scan.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: cache-lookup-wrong-operator
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
SSTable key lookup cached the file position of an exact match for any operator. For a `GT` (strictly greater-than) range query, using the cached position of the boundary key would return that key as the first result, violating the exclusive lower-bound semantics. The fix wraps the key-cache lookup in an `if (op == Operator.EQ || op == Operator.GE)` guard so that `GT` always falls through to the index scan, which correctly positions past the boundary key.

## Example
```java
// BEFORE
Pair<Descriptor, DecoratedKey> unifiedKey = new Pair<>(descriptor, decoratedKey);
Long cachedPosition = getCachedPosition(unifiedKey);
if (cachedPosition != null)
    return cachedPosition;
```
```java
// AFTER
if (op == Operator.EQ || op == Operator.GE)
{
    Pair<Descriptor, DecoratedKey> unifiedKey = new Pair<>(descriptor, decoratedKey);
    Long cachedPosition = getCachedPosition(unifiedKey);
    if (cachedPosition != null)
        return cachedPosition;
}
```
