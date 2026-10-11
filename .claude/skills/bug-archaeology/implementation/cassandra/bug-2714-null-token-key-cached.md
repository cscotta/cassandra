# Bug 2714: Token-only DecoratedKeys cached with null key cause NPE on later lookup

**Commit**: 86f6184d0d · **Ticket**: CASSANDRA-2416

## Detection Rule
When caching objects that may have null fields, assert or guard against the null before inserting into the cache.

## Code Smell
`cacheKey` called `ByteBufferUtil.clone(key.key)` defensively inside a null-check, allowing a null key to be stored; downstream cache consumers then dereference the key bytes and NPE.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: null-key-cached
- severity: crash
- subsystem: storage-engine

## Root Cause
`DecoratedKey` can be constructed with a null `key` field when only a token is needed (e.g., during range scans). `SSTableReader.cacheKey()` previously allowed `key.key == null` and stored a null-key entry; later `getPosition()` calls that hit the cache tried to use the null key bytes and threw NPE. The fix adds an assertion inside `cacheKey` and a null guard at the call site so token-only keys are never inserted.

## Example
```java
// BEFORE — null key is stored when key.key == null
DecoratedKey copiedKey = new DecoratedKey(
    key.token, key.key == null ? null : ByteBufferUtil.clone(key.key));
keyCache.put(..., copiedKey, info);
```
```java
// AFTER — assert + call-site guard prevents null-key caching
assert key.key != null;
DecoratedKey copiedKey = new DecoratedKey(key.token, ByteBufferUtil.clone(key.key));
// ...
if (decoratedKey.key != null)
    cacheKey(decoratedKey, dataPosition);
```
