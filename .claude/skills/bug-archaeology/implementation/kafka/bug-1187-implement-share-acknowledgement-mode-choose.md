**Commit**: f737ef31d9 · **Ticket**: KAFKA-18900
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Choose the acknowledgement mode based on the config
(`share.acknowledgement.mode`) and not on the basis of how the user
designs the application. - The default value of the config is `IMPLICIT`, so if any
empty/null/invalid value is configured, then the mode defaults to
`IMPLICIT`. - Removed AcknowledgementModes `UNKNOWN` and `PENDING` as they are no
longer required. - Added code to ensure if the application has any unacknowledged records
in a batch in "`explicit`" mode, then it will throw an
`IllegalStateException`.

**Before/After**
```java
// Before
             ShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer("group1")) {
             ShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer("group1")) {
             ShareConsumer<byte[], byte[]> shareConsumer1 = createShareConsumer("group1");
            // The 3rd record should be reassigned to 2nd consumer when it polls, kept higher wait time
            // as time out for locks is 15 secs.
    @ClusterTest
    public void testImplicitModeNotTriggeredByPollWhenNoAcksToSend() throws InterruptedException {
        alterShareAutoOffsetReset("group1", "earliest");
        try (Producer<byte[], byte[]> producer = createProducer();
             ShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer("group1")) {

            shareConsumer.subscribe(Set.of(tp.topic()));

// After
    private static final String EXPLICIT = "explicit";
    private static final String IMPLICIT = "implicit";
             ShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer("group1", Map.of(ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_CONFIG, EXPLICIT))) {
             ShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer("group1", Map.of(ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_CONFIG, EXPLICIT))) {
             ShareConsumer<byte[], byte[]> shareConsumer1 = createShareConsumer("group1", Map.of(ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_CONFIG, EXPLICIT));
            ConsumerRecord<byte[], byte[]> thirdRecord = iterator.next();
            shareConsumer1.acknowledge(thirdRecord, AcknowledgeType.RELEASE);
            // The 3rd record should be reassigned to 2nd consumer when it polls.
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18900: implement share.acknowledgement.mode to choose acknowledgement mode (#19417).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
