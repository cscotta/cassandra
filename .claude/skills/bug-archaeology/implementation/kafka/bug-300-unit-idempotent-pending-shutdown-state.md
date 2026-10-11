**Commit**: 73cb15e72c · **Ticket**: KAFKA-16600
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
The change includes a unit test that reliably fails before the fix from
KAFKA-17379 and reliably passes afterward. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
    private void closeToError() {

// After
    void closeToError() {
    @Test
    public void shouldHandleCloseAfterErrorState() throws Exception {
        // Regression test for the race condition bug fixed by KAFKA-17379 that also fixed KAFKA-16600.
        prepareStreams();
        final AtomicReference<StreamThread.State> state1 = prepareStreamThread(streamThreadOne, 1);
        final AtomicReference<StreamThread.State> state2 = prepareStreamThread(streamThreadTwo, 2);
        prepareThreadState(streamThreadOne, state1);
        prepareThreadState(streamThreadTwo, state2);

        try (final KafkaStreams streams = new KafkaStreams(getBuilderWithSource().build(), props, supplier, time)) {
            streams.start();
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16600: add unit test for idempotent pending_shutdown state transition (#21671).

**Test**
Test modified in KafkaStreamsTest.java covering the corrected behaviour.
