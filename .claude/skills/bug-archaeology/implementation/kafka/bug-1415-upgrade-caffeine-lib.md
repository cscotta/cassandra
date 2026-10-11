**Commit**: 934b0159bb · **Ticket**: KAFKA-18089
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
- Fixed the RemoteIndexCacheTest that fails with caffeine > 3.1.1

Reviewers: Luke Chen <showuon@gmail.com>, Kamal Chandraprakash <kamal.chandraprakash@gmail.com>

**Before/After**
```java
// Before
- caffeine-3.1.1
    // Calling getIndex on the same entry should not call rsm#fetchIndex again, but it should retrieve from cache
    verifyFetchIndexInvocation(count = 3)
    def verifyEntryIsEvicted(metadataToVerify: RemoteLogSegmentMetadata, entryToVerify: Entry): Unit = {
      // wait until `entryToVerify` is marked for deletion
      TestUtils.waitUntilTrue(() => entryToVerify.isMarkedForCleanup,
      TestUtils.waitUntilTrue(() => entryToVerify.isCleanStarted,
      // verify no index files for `entryToVerify` on remote cache dir
      TestUtils.waitUntilTrue(() => getIndexFileFromRemoteCacheDir(cache, remoteOffsetIndexFileName(metadataToVerify)).isEmpty,
        s"Offset index file for evicted entry should not be present on disk at ${cache.cacheDir()}")
      TestUtils.waitUntilTrue(() => getIndexFileFromRemoteCacheDir(cache, remoteTimeIndexFileName(metadataToVerify)).isEmpty,
        s"Time index file for evicted entry should not be present on disk at ${cache.cacheDir()}")

// After
- caffeine-3.1.8
    // Calling getIndex on the same entry may call rsm#fetchIndex or not, it only depend on cache implementation so
    // we only need to verify the number of calling is in our range.
    verifyFetchIndexInvocationWithRange(lower = 3, upper = 4)
    def getRemoteLogSegMetadataIsKept(metadataToVerify: List[RemoteLogSegmentMetadata]): List[RemoteLogSegmentMetadata] = {
      metadataToVerify.filter(s => { cache.internalCache().asMap().containsKey(s.remoteLogSegmentId().id())})
    }
    def verifyEntryIsEvicted(metadataToVerify: List[RemoteLogSegmentMetadata], entriesToVerify: List[Entry],
                             numOfMarkAsDeleted: Int): (List[RemoteLogSegmentMetadata], List[Entry]) = {
      TestUtils.waitUntilTrue(() => entriesToVerify.count(_.isMarkedForCleanup).equals(numOfMarkAsDeleted),

      TestUtils.waitUntilTrue(() => entriesToVerify.count(_.isCleanStarted).equals(numOfMarkAsDeleted),
```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18089: upgrade caffeine lib to 3.1.8 (#18004).

**Test**
Test modified in RemoteIndexCacheTest.scala covering the corrected behaviour.
