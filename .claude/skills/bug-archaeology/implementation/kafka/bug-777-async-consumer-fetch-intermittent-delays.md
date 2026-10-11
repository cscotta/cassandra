**Commit**: f922ff6d1f · **Ticket**: KAFKA-19259
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
There’s a difference in the two consumers’ `pollForFetches()` methods in
this case: `ClassicKafkaConsumer` doesn't block waiting for data in the
fetch buffer, but `AsyncKafkaConsumer` does. In `ClassicKafkaConsumer.pollForFetches()`, after enqueuing the `FETCH`
request, the consumer makes a call to `ConsumerNetworkClient.poll()`. In
most cases `poll()` returns almost immediately because it successfully
sent the `FETCH` request. So even when the `pollTimeout` value is, e.g.

**Before/After**
```java
// Before
// (no removals)

// After
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.fail;
    @ClusterTest
    public void testClassicConsumerStallBetweenPoll() throws Exception {
        testStallBetweenPoll(GroupProtocol.CLASSIC);
    }

    @ClusterTest
    public void testAsyncConsumerStallBetweenPoll() throws Exception {
        testStallBetweenPoll(GroupProtocol.CONSUMER);
    }

```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19259: async consumer fetch intermittent delays on console consumer (#19980).

**Test**
Test modified in PlaintextConsumerTest.java covering the corrected behaviour.
