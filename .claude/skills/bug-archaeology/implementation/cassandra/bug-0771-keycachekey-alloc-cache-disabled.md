# Bug 0771: KeyCacheKey Allocated Even When Key Cache Is Disabled

**Commit**: 3f1a3b2f60 · **Ticket**: CASSANDRA-13084
**Subsystem**: SSTable / Key Cache
**Category**: Logic error in condition

---

## Root Cause
`BigTableReader.getPosition()` unconditionally constructed a `KeyCacheKey` object — involving a `ByteBuffer` copy of the serialized key — before calling `getCachedPosition(KeyCacheKey, boolean)`, which then immediately returned `null` if the key cache was disabled or empty. The allocation happened every time a key lookup was performed, producing needless garbage even when the feature was completely turned off.

Similarly, `SSTableReader.getCachedPosition(DecoratedKey, boolean)` always created a `KeyCacheKey` before delegating to the two-argument overload that held the actual guard. The fix adds a `keyCacheEnabled()` helper and gates all `KeyCacheKey` construction behind it so no object is allocated unless the cache will actually be consulted.

### Before / After
```java
// BEFORE (BigTableReader): allocates KeyCacheKey unconditionally
DecoratedKey decoratedKey = (DecoratedKey)key;
KeyCacheKey cacheKey = new KeyCacheKey(metadata.ksAndCFName, descriptor, decoratedKey.getKey());
RowIndexEntry cachedPosition = getCachedPosition(cacheKey, updateCacheAndStats);

// AFTER: delegates through getCachedPosition(DecoratedKey, boolean) which checks first
RowIndexEntry cachedPosition = getCachedPosition((DecoratedKey)key, updateCacheAndStats);

// BEFORE (SSTableReader): allocates KeyCacheKey before guard
public RowIndexEntry getCachedPosition(DecoratedKey key, boolean updateStats)
{
    return getCachedPosition(new KeyCacheKey(..., key.getKey()), updateStats);
}

// AFTER: guard first, allocate only when needed
public RowIndexEntry getCachedPosition(DecoratedKey key, boolean updateStats)
{
    if (keyCacheEnabled())
        return getCachedPosition(getCacheKey(key), updateStats);
    return null;
}
```

---

## How to Spot
Look for object constructors called before a guard that would have short-circuited the method anyway. The pattern is especially common in cache/index lookup paths where an expensive key object is built as an argument to a method that starts with an enabled-check. Moving the guard to the outermost callsite eliminates the allocation on the fast-reject path.

---

## Test
No test added.
