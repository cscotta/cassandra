**Commit**: 43a25043dd · **Ticket**: KAFKA-19567
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
In the current implementation, some delayed share fetch operations get
trapped in the delayed share fetch purgatory when the partition
leaderships change during share consumption. This is because there is no
check in code to make sure the current broker is still the partition
leader corresponding to the share partitions. So, when leadership
changes, the share partitions cannot be acquired, because they have
already been fenced, and tryComplete returns false. Although the
operatio does get completed when the timer expires for it, but it is too
late by then, and the operation get stuck in the watchers list waiting
for it to get purged when estimated operations increase to more than
1000.

**Before/After**
```java
// Before
     * Case d: All remote storage read requests completed
                replicaManager.getPartitionOrException(topicIdPartition.topicPartition());
            } catch (NotLeaderOrFollowerException e) { // Case c
        if (canComplete || pendingRemoteFetchesOpt.get().isDone()) { // Case d

// After
import org.apache.kafka.raft.errors.NotLeaderException;
            // At this point, there could be delayed requests sitting in the purgatory which are waiting on
            // DelayedShareFetchPartitionKeys corresponding to partitions, whose leader has been changed to a different broker.
            // In that case, such partitions would not be able to get acquired, and the tryComplete will keep on returning false.
            // Eventually the operation will get timed out and completed, but it might not get removed from the purgatory.
            // This has been eventually left it like this because the purging mechanism will trigger whenever the number of completed
            // but still being watched operations is larger than the purge interval. This purge interval is defined by the config
            // share.fetch.purgatory.purge.interval.requests and is 1000 by default, thereby ensuring that such stale operations do not
            // grow indefinitely.
     * Case d: This broker is no longer the leader or follower of the partition it tries to fetch
     * Case e: All remote storage read requests completed
                Partition partition = replicaManager.getPartitionOrException(topicIdPartition.topicPartition());
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19567: added the check for underlying partition being the leader in delayedsharefetch trycomplete method (#20280).

**Test**
Test modified in DelayedShareFetchTest.java covering the corrected behaviour.
