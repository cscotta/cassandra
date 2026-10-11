**Commit**: 2a370ed721 · **Ticket**: KAFKA-19037
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
The consumer adaptations for the new Streams rebalance protocol need to
be integrated into the Streams code. This commit does the following:
- creates an async Kafka consumer
  - with a Streams heartbeat request manager
  - with a Streams membership manager
- integrates consumer code with the Streams membership manager and the
Streams heartbeat request manager
- processes the events from the consumer network thread (a.k.a. background thread)
  that request the invocation of the "on tasks revoked", "on  tasks
assigned", and "on all tasks lost"
  callbacks
- executes the callbacks
- sends to the consumer network thread the events signalling the
execution of the callbacks
- adapts SmokeTestDriv...

**Before/After**
```java
// Before

    AsyncKafkaConsumer(final ConsumerConfig config,
                       final Deserializer<K> keyDeserializer,
                       final Deserializer<V> valueDeserializer) {
            new LinkedBlockingQueue<>()
                       final LinkedBlockingQueue<BackgroundEvent> backgroundEventQueue) {
                    memberStateListener
            memberStateListener

// After

    public AsyncKafkaConsumer(final ConsumerConfig config,
                              final Deserializer<K> keyDeserializer,
                              final Deserializer<V> valueDeserializer,
                              final Optional<StreamsRebalanceData> streamsRebalanceData,
                              final Optional<StreamsRebalanceEventsProcessor> streamsRebalanceEventsProcessor) {
            new LinkedBlockingQueue<>(),
            streamsRebalanceData,
            streamsRebalanceEventsProcessor
    @SuppressWarnings({"this-escape"})
                       final LinkedBlockingQueue<BackgroundEvent> backgroundEventQueue,
                       final Optional<StreamsRebalanceData> streamsRebalanceData,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19037: integrate consumer-side code with streams (#19377).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
