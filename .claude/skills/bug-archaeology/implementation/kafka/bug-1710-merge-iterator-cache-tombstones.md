**Commit**: 26771a6d28 · **Ticket**: KAFKA-18326
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
See https://issues.apache.org/jira/browse/KAFKA-18326 for more information. The main bug here is that in the old implementation, deleted cache entries would be skipped so long as they didn't equal the next store key, which resulted in potentially skipping tombstones for future keys in the store. Reviewers: Guozhang Wang <guozhang.wang.us@gmail.com>, Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
            if (storeIterator.hasNext()) {
                final KS nextStoreKey = storeIterator.peekNextKey();
                // advance the store iterator if the key is the same as the deleted cache key
                if (compare(cacheIterator.peekNextKey(), nextStoreKey) == 0) {
                    storeIterator.next();
                }
            cacheIterator.next();
            cache.put(namespace, aBytes, new LRUCacheEntry(aByte));

// After
            if (!storeIterator.hasNext()) {
                // if storeIterator is exhausted, we can just skip over every tombstone
                // in the cache since they don't shadow any valid key
                cacheIterator.next();
                continue;
            }

            final KS nextStoreKey = storeIterator.peekNextKey();
            final int compare = compare(cacheIterator.peekNextKey(), nextStoreKey);

            if (compare == 0) {
                // next cache entry is a valid tombstone for the next store key
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18326: fix merge iterator with cache tombstones (#18287).

**Test**
Test modified in MergedSortedCacheKeyValueBytesStoreIteratorTest.java covering the corrected behaviour.
