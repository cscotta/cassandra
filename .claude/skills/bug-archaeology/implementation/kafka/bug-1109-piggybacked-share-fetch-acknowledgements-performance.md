**Commit**: ac9520b922 · **Ticket**: KAFKA-19227
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
The PR fixes the issue when ShareAcknowledgements are piggybacked on
ShareFetch. The current default configuration in clients sets `batch
size` and `max fetch records` as per the `max.poll.records` config,
default 500. Which means all records in a single poll will be fetched
and acknowledged. Also the default configuration for inflight records in
a partition is 200.

**Before/After**
```java
// Before
                maybeUpdateCachedStateAndOffsets();
    private void maybeUpdateCachedStateAndOffsets() {
                return;
            // If lastOffsetAcknowledged is -1, this means we cannot move out startOffset ahead
                return;

// After
    /**
     * The delayed share fetch key is used to track the delayed share fetch requests that are waiting
     * for the respective share partition.
     */
    private final DelayedShareFetchKey delayedShareFetchKey;

        this.delayedShareFetchKey = new DelayedShareFetchGroupKey(groupId, topicIdPartition);
                // Avoid triggering the listener for waiting share fetch requests in purgatory as the
                // share partition manager keeps track of same and will trigger the listener for the
                // respective share partition.
            // There can be a pending delayed share fetch requests for the share partition which are waiting
            // on the startOffset to move ahead, hence track if the state is updated in the cache. If
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19227: piggybacked share fetch acknowledgements performance issue (#19612).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
