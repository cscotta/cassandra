**Commit**: 51fa1601c1 · **Ticket**: KAFKA-20333
**Subsystem**: clients/clients-integration-tests/src/test/java/org/apache/kafka/clients/consumer/PlaintextConsumerPollTest.java
**Category**: Wrong constant / default value

**Root cause**: A consumer poll test used `MAX_POLL_INTERVAL_MS_CONFIG = 100` (100 ms). This is intentionally low to make consumers leave the group when they stop polling, but it is so low that stable consumers in the same test can exhaust their poll timer during a rebalance, causing spurious failures. Increasing the value to 1000 ms (matching similar tests in the file) provides enough margin for rebalances to complete without triggering unintended group leaves.

**Before/After**:
```java
// BEFORE – 100 ms too aggressive; stable consumers expire during rebalance
MAX_POLL_INTERVAL_MS_CONFIG, 100

// AFTER – 1000 ms gives rebalance time to complete
MAX_POLL_INTERVAL_MS_CONFIG, 1000
```

**How to spot**: In consumer group tests, if the poll-interval timeout is set to a value comparable to the expected rebalance duration, any slow CI machine or noisy environment can trigger a false group-leave. Search for small `max.poll.interval.ms` values (< 500 ms) in integration tests that also involve rebalances.

**Test**: No new scenario. The fix is a timeout constant adjustment validated by the test passing consistently on trunk.
