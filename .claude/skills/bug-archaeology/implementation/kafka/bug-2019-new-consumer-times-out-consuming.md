**Commit**: 5725a51453 · **Ticket**: KAFKA-16460
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before

        verify(subscriptionState).markPendingRevocation(anySet());

// After
        // Mark partitions as pending revocation to stop fetching from the partitions (no new
        // fetches sent out, and no in-flight fetches responses processed).
        markPendingRevocationToPauseFetching(revokedPartitions);

import org.mockito.InOrder;
import static org.mockito.Mockito.inOrder;
        verify(subscriptionState, times(2)).markPendingRevocation(Set.of(new TopicPartition("topic1", 0)));
        InOrder inOrder = inOrder(subscriptionState, commitRequestManager);
        inOrder.verify(subscriptionState).markPendingRevocation(Set.of(new TopicPartition("topic1", 0)));
        inOrder.verify(commitRequestManager).maybeAutoCommitSyncBeforeRevocation(anyLong());
        inOrder.verify(subscriptionState).markPendingRevocation(Set.of(new TopicPartition("topic1", 0)));
        verify(subscriptionState, times(2)).markPendingRevocation(Set.of(new TopicPartition("topic1", 0)));
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16460: new consumer times out consuming records in multiple consumer_test.py system tests (#17777).

**Test**
Test modified in ConsumerMembershipManagerTest.java covering the corrected behaviour.
