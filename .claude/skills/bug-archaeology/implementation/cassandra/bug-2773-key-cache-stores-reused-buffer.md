# Bug 2773: Key Cache and Row Cache Store Reference to Recycled Key Buffer

**Commit**: 64e1c79501 · **Ticket**: n/a (follow-up to CASSANDRA-2102)

## Detection Rule
When caching a `DecoratedKey` in a long-lived cache, always clone the key's `ByteBuffer` to avoid holding a reference into a reusable I/O buffer that will be overwritten on the next read.

## Code Smell
`cacheKey` passes `key.key` (or `unifiedKey.right.key`) directly into the cache without cloning, while a comment says the TFFT reader reuses the underlying buffer for the key.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: cached-reference-to-reused-buffer
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`SSTableReader.cacheKey` stored a `DecoratedKey` whose backing `ByteBuffer` pointed into the SSTable file's I/O buffer. When that buffer was reused for subsequent reads, the cached key was silently overwritten with different data, causing cache lookups to match wrong rows. A secondary NPE existed when `key.key == null` for GE range scans: the `clone` call was not null-checked. The fix clones the key buffer for all cache insertions and adds a null guard.

## Example
```java
// BEFORE
keyCache.put(new Pair<>(descriptor,
    new DecoratedKey(key.token, ByteBufferUtil.clone(key.key))), info);
// ... but called with unifiedKey.right which wasn't cloned
cacheKey(unifiedKey.right, dataPosition);
```
```java
// AFTER
DecoratedKey copiedKey = new DecoratedKey(key.token,
    key.key == null ? null : ByteBufferUtil.clone(key.key));
keyCache.put(new Pair<>(descriptor, copiedKey), info);
// ... called with the original decoratedKey so clone happens here
cacheKey(decoratedKey, dataPosition);
```
