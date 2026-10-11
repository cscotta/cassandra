**Commit**: 0491e1124e · **Ticket**: KAFKA-19892
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Part of KIP-1222. Use the acquisitionLockDurationMs in the
ShareAcknowledge response to update the value in the share consumer when
renew acknowledgements are used. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
                // if there is a leader and no in-flight requests, issue a new fetch
                                log.debug("Leader for the partition is down or has changed, failing Acknowledgements for partition {}", tip);
                                maybeSendShareAcknowledgementEvent(Map.of(tip, acks), true);
        // Iterate over the share session handlers and build a list of UnsentRequests

// After
            assertEquals(Optional.of(15000), shareConsumer.acquisitionLockTimeoutMs());

            // The updated acquisition lock timeout is only applied when the next poll is called.
            alterShareRecordLockDurationMs("group1", 25000);
                assertEquals(Optional.of(15000), shareConsumer.acquisitionLockTimeoutMs());
            assertEquals(Optional.of(25000), shareConsumer.acquisitionLockTimeoutMs());
    private void alterShareRecordLockDurationMs(String groupId, int newValue) {
        ConfigResource configResource = new ConfigResource(ConfigResource.Type.GROUP, groupId);
        Map<ConfigResource, Collection<AlterConfigOp>> alterEntries = new HashMap<>();
        alterEntries.put(configResource, List.of(new AlterConfigOp(new ConfigEntry(
            GroupConfig.SHARE_RECORD_LOCK_DURATION_MS_CONFIG, Integer.toString(newValue)), AlterConfigOp.OpType.SET)));
        AlterConfigsOptions alterOptions = new AlterConfigsOptions();
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19892: client use of shareacknowledge acquisition lock timeout (#20905).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
