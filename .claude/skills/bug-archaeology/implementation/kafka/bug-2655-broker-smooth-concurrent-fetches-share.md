**Commit**: 6ff51bc388 · **Ticket**: KAFKA-17210
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Identified a couple of reliability issues with broker code for share groups 

1. Broker seems to get stuck at times when using multiple share consumers due to a corner case where the second last fetch request did not contain any topic partition to fetch, because of which the broker could never complete the last request. This results in a share fetch request getting stuck. 2.

**Before/After**
```java
// Before
        this.fetchQueue = new ConcurrentLinkedQueue<>();
    private void maybeProcessFetchQueue() {

// After
            // In case the persister read state RPC result contains no AVAILABLE records, we can update cached state
            // and start/end offsets.
            maybeUpdateCachedStateAndOffsets();
            ConcurrentLinkedQueue<ShareFetchPartitionData> fetchQueue,
        this.fetchQueue = fetchQueue;
    // Visible for testing.
    void maybeProcessFetchQueue() {
                if (!fetchQueue.isEmpty())
                    maybeProcessFetchQueue();
import java.util.concurrent.ConcurrentLinkedQueue;
import static org.mockito.Mockito.verify;
    @Test
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17210: broker fixes for smooth concurrent fetches on share partition (#16711).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
