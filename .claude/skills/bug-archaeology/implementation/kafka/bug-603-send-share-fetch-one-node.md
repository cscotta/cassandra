**Commit**: 108ad6eeeb · **Ticket**: KAFKA-19767
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
*What*

- After KIP-1206, when `record_limit` mode was introduced, we ideally
want no more than the #records in the `maxRecords` field in
`ShareFetchRequest`. - Currently, the client broadcasts the share fetch requests to all nodes
which host the leaders of the partitions that it is subscribed to. - The application thread would be woken up after the first response
arrives, but meanwhile the responses from other nodes could bring in
those many #records next and would wait in the buffer, that would mean
we are wasting the acquisition locks for these records which are
waiting. - Instead we would want to only send the next request when we poll
again.

**Before/After**
```java
// Before
        }).collect(Collectors.toList());

// After
import org.apache.kafka.clients.consumer.ShareAcquireMode;
    private final AtomicInteger fetchRecordsNodeId = new AtomicInteger(-1);
                // If we have not chosen a node for fetching records yet,
                // choose now, and rotate the assigned partitions so the next poll starts on a different partition.
                // This is only applicable for record_limit mode.
                if (isShareAcquireModeRecordLimit() && fetchRecordsNodeId.compareAndSet(-1, node.id())) {
                    subscriptions.movePartitionToEnd(partition);
                }

            // For record_limit mode, we only send a full ShareFetch to a single node at a time.
            // We prepare to build ShareFetch requests for all nodes with session handlers to permit
            // piggy-backing of acknowledgements, and also to adjust the topic-partitions
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19767 - send share-fetch one-node at a time for record_limit mode (#20855).

**Test**
Test modified in ShareConsumeRequestManagerTest.java covering the corrected behaviour.
