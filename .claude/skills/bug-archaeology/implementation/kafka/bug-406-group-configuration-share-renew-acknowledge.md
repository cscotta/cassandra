**Commit**: ee2204f213 · **Ticket**: KAFKA-20161
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Introduce the `share.renew.acknowledge.enable` group configuration
(KIP-1222) to control whether renew acknowledgements are permitted for a
share group. - Add new boolean dynamic group config
`share.renew.acknowledge.enable` (default false) in `GroupConfig`
- When disabled, RENEW acknowledgements are rejected with
`INVALID_RECORD_STATE` error code
 - Update existing RENEW tests to explicitly enable the config
 - Add unit tests and integration test for the disabled-by-default
behavior

Reviewers: Andrew Schofield <aschofield@confluent.io>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.coordinator.group.GroupConfigManager;
    /**
     * The method is used to get the record lock duration for the group. If the group config is present,
     * then the record lock duration is returned. Otherwise, the default value is returned.
     *
     * @param groupConfigManager The group config manager.
     * @param groupId The group id for which the record lock duration is to be fetched.
     * @param defaultValue The default value to be returned if the group config is not present.
     * @return The record lock duration for the group.
     */
    public static int recordLockDurationMsOrDefault(GroupConfigManager groupConfigManager, String groupId, int defaultValue) {
        if (groupConfigManager.groupConfig(groupId).isPresent()) {

// After
    @ClusterTest
    public void testRenewAcknowledgementDisabled() {
        alterShareAutoOffsetReset("group1", "earliest");
        alterShareRenewAcknowledgeEnable("group1", false);
        try (Producer<byte[], byte[]> producer = createProducer();
             ShareConsumer<byte[], byte[]> shareConsumer = createShareConsumer(
                 "group1",
                 Map.of(ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_CONFIG, EXPLICIT))
        ) {
            ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(tp.topic(), tp.partition(), null, "key".getBytes(), "Message".getBytes());
            producer.send(record);
            producer.flush();
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20161 group configuration for share.renew.acknowledge.enable (#21467).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
