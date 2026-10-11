**Commit**: 3d8a90a47b · **Ticket**: KAFKA-20112
**Subsystem**: clients/clients-integration-tests/src/test/java/org/apache/kafka/clients/consumer/PlaintextConsumerPollTest.java
**Category**: Race condition / missing synchronization

**Root cause**: The test `testConsumerCallbackAndPositionTest` waited for a rebalance event (`awaitRebalance`) to verify that `position()` was accessible in a revocation callback. However, a rebalance completing does not guarantee that the consumer has fetched any records or initialized offsets for the assigned partition. A partition could be assigned and immediately revoked in a second rebalance before the consumer ever polled, leaving the position at the uninitialized sentinel value and causing a flaky `assertEquals(0, committedPosition.get())` assertion.

**Before/After**:
```java
// BEFORE – rebalance completion alone does not guarantee position is initialized
awaitRebalance(consumer, listener);
consumer.subscribe(List.of("otherTopic"), listener);
awaitRebalance(consumer, listener);
assertEquals(0, committedPosition.get());

// AFTER – wait for at least one record to ensure position is set
awaitNonEmptyRecords(consumer, tp, 100);
consumer.subscribe(List.of(otherTopic), listener);
awaitNonEmptyRecords(consumer, tpOther, 100);
assertEquals(1, committedPosition.get());
```

**How to spot**: Any consumer test that checks `position()` in a rebalance callback without first confirming a successful poll is vulnerable to this race. The position is only valid after the first fetch response for a partition.

**Test**: The fix itself is within the test. Pre-populating both `tp` and `tpOther` with one record each and using `awaitNonEmptyRecords` ensures the consumer has a valid position before the next rebalance is triggered. The expected committed position changes from 0 to 1 accordingly.
