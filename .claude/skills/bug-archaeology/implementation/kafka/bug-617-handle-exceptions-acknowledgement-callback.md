**Commit**: 403f6c1caa · **Ticket**: KAFKA-19840
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Catch exceptions from the acknowledgement commit callback. Previously,
the exceptions were thrown by the methods of the KafkaShareConsumer
interface, but this was not described in the KIP and did not make sense
in practice. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, ShivsundarR
<shr@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.test.api.Flaky;
import org.junit.jupiter.api.Disabled;
     * Test to verify that the acknowledgement commit callback can throw an exception, and it is propagated
    @Flaky("KAFKA-19840") // https://issues.apache.org/jira/browse/KAFKA-19840
            shareConsumer.setAcknowledgementCommitCallback(new TestableAcknowledgementCommitCallbackThrows<>());
            AtomicBoolean exceptionThrown = new AtomicBoolean(false);
                } catch (org.apache.kafka.common.errors.OutOfOrderSequenceException e) {
                    exceptionThrown.set(true);
                return exceptionThrown.get();
            }, DEFAULT_MAX_WAIT_MS, 100L, () -> "Failed to receive expected exception");
    private static class TestableAcknowledgementCommitCallbackThrows<K, V> implements AcknowledgementCommitCallback {
    @Disabled("KAFKA-19840") // https://issues.apache.org/jira/browse/KAFKA-19840

// After
import org.apache.kafka.test.NoRetryException;
     * Test to verify that the acknowledgement commit callback can throw an exception, and it is not propagated
            AtomicBoolean callbackCalled = new AtomicBoolean(false);
            shareConsumer.setAcknowledgementCommitCallback(new TestableAcknowledgementCommitCallbackThrows(callbackCalled));
                } catch (Exception e) {
                    throw new NoRetryException(e);
                return callbackCalled.get();
            }, DEFAULT_MAX_WAIT_MS, 100L, () -> "Received unexpected exception or callback not called");
    private static class TestableAcknowledgementCommitCallbackThrows implements AcknowledgementCommitCallback {
        private final AtomicBoolean callbackCalled;

        public TestableAcknowledgementCommitCallbackThrows(AtomicBoolean callbackCalled) {
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19840: handle exceptions in acknowledgement callback (#20880).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
