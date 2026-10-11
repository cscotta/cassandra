**Commit**: 2c347380b7 · **Ticket**: KAFKA-19694
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
In the consumer, we invoke the consumer rebalance onPartitionRevoked or
onPartitionLost callbacks, when the consumer closes. The point is that
the application may want to commit, or wipe the state if we are closing
unsuccessfully. In the StreamsRebalanceListener, we did not implement this behavior,
which means when closing the consumer we may lose some progress, and in
the worst case also miss that we have to wipe our local state state
since we got fenced. In this PR we implement StreamsRebalanceListenerInvoker, very similarly
to ConsumerRebalanceListenerInvoker and invoke it in Consumer.close.

**Before/After**
```java
// Before
        private Optional<StreamsRebalanceListener> streamsRebalanceListener = Optional.empty();
        private final Optional<StreamsRebalanceData> streamsRebalanceData;

        public BackgroundEventProcessor() {
            this.streamsRebalanceData = Optional.empty();
        }

        public BackgroundEventProcessor(final Optional<StreamsRebalanceData> streamsRebalanceData) {
            this.streamsRebalanceData = streamsRebalanceData;
        }

        private void setStreamsRebalanceListener(final StreamsRebalanceListener streamsRebalanceListener) {

// After
            final Optional<Exception> exceptionFromCallback = Optional.ofNullable(streamsRebalanceListenerInvoker().invokeTasksRevoked(activeTasksToRevoke));
            final Optional<Exception> exceptionFromCallback = Optional.ofNullable(streamsRebalanceListenerInvoker().invokeTasksAssigned(assignment));
            final Optional<KafkaException> error = exceptionFromCallback.map(e -> ConsumerUtils.maybeWrapAsKafkaException(e, "Task assignment callback throws an error"));
            final Optional<Exception> exceptionFromCallback = Optional.ofNullable(streamsRebalanceListenerInvoker().invokeAllTasksLost());
            final Optional<KafkaException> error = exceptionFromCallback.map(e -> ConsumerUtils.maybeWrapAsKafkaException(e, "All tasks lost callback throws an error"));
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19694: trigger streamsrebalancelistener in consumer.close (#20511).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
