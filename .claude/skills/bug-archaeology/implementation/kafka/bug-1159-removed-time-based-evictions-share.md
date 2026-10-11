**Commit**: 22c5794bc3 · **Ticket**: KAFKA-19159
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Currently the share session cache is desgined like the fetch session
cache. If the cache is full and a new share session is trying to get get
initialized, then the sessions which haven't been touched for more than
2minutes are evicted. This wouldn't be right for share sessions as the
members also hold locks on the acquired records, and session eviction
would mean theose locks will need to be dropped and the corresponding
records re-delivered. This PR removes the time based eviction logic for
share sessions.

**Before/After**
```java
// Before
                ShareSessionKey responseShareSessionKey = cache.maybeCreateSession(groupId, reqMetadata.memberId(),
                        time.milliseconds(), cachedSharePartitions);
                cache.touch(shareSession, time.milliseconds());
                cache.touch(shareSession, time.milliseconds());
        config.shareGroupConfig.shareGroupMaxGroups * config.groupCoordinatorConfig.shareGroupMaxSize,
        KafkaBroker.MIN_INCREMENTAL_FETCH_SESSION_EVICTION_MS)
import static org.junit.jupiter.api.Assertions.assertNull;
        ShareSessionCache cache = new ShareSessionCache(10, 1000);
        ShareSessionCache cache = new ShareSessionCache(10, 1000);

// After
                ShareSessionKey responseShareSessionKey = cache.maybeCreateSession(groupId, reqMetadata.memberId(), cachedSharePartitions);
                cache.updateNumPartitions(shareSession);
                cache.updateNumPartitions(shareSession);
        config.shareGroupConfig.shareGroupMaxGroups * config.groupCoordinatorConfig.shareGroupMaxSize
      )
        ShareSessionCache cache = new ShareSessionCache(10);
        ShareSessionCache cache = new ShareSessionCache(10);
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19159: removed time based evictions for share sessions (#19500).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
