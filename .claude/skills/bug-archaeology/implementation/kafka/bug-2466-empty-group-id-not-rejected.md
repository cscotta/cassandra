**Commit**: 70dd577286 · **Ticket**: KAFKA-15909
**Subsystem**: clients/consumer/internals/ClassicKafkaConsumer
**Category**: Missing null / bounds check

**Root cause**
`ClassicKafkaConsumer` (the legacy consumer) emitted only a deprecation warning when `group.id` was set to an empty or whitespace-only string, while `AsyncKafkaConsumer` (the new consumer) already threw `InvalidGroupIdException`. This inconsistency meant that users of the classic consumer could silently continue using an invalid group ID that would cause undefined coordination behavior. In 4.0, both consumers must reject empty/whitespace group IDs with a clear exception.

**Before/After**
```java
// Before (just a warning)
groupId.ifPresent(groupIdStr -> {
    if (groupIdStr.isEmpty()) {
        log.warn("Support for using empty group id is deprecated...");
    }
});

// After (throws immediately)
if (groupRebalanceConfig.groupId != null && groupRebalanceConfig.groupId.isEmpty()) {
    throw new InvalidGroupIdException("The configured " + GROUP_ID_CONFIG
            + " should not be an empty string or whitespace.");
}
```

**How to spot**
When two consumer implementations share the same configuration validation path, verify that both enforce identical invariants. A warning-only check in the legacy path while the new path throws an exception creates a hidden behavioral divergence that only manifests after users migrate consumer protocols.

**Test**
`testEmptyGroupId` and `testGroupIdWithWhitespace` added to `KafkaConsumerTest`.
