**Commit**: c77a96d132 · **Ticket**: KAFKA-20157
**Subsystem**: storage/src/main/java/org/apache/kafka/server/log/remote/storage/RemoteLogManager.java
**Category**: Wrong constant / default value

**Root cause**: When no remote log segments existed for a partition, `RLMExpirationTask.cleanupExpiredRemoteLogSegments()` returned early without ever calling `calculateSizeInPercent`, so the new `RetentionSizeInPercent` and `LocalRetentionSizeInPercent` gauges stayed at their initial value of zero even when the local log was non-empty and approaching its retention limit. Additionally, the pre-existing `buildRetentionSizeData` path only computed percentages in two of the three code branches, again leaving gauges stale when total size was within retention.

**Before/After**:
```java
// BEFORE – early return skips percentage calculation entirely
if (stats.metadataCount == 0) {
    updateMetadataCountAndLogSizeWith(0, 0);
    return;          // gauges never updated
}
...
if (retentionSize < 0 || (onlyLocalLogSegmentsSize + ...) <= retentionSize) {
    return Optional.empty();  // gauges never updated in this branch
}

// AFTER – calculate metrics even when returning early
if (stats.metadataCount == 0) {
    updateMetadataCountAndLogSizeWith(0, 0);
    calculateSizeInPercent(log.size(), log.config().retentionSize,
                           log.size(), log.config().localRetentionBytes());
    return;
}
...
if (totalEstimateSize <= retentionSize) {
    calculateSizeInPercent(totalEstimateSize, retentionSize,
                           localLogSegmentsSize, localRetentionBytes);
    return Optional.empty();
}
```

**How to spot**: Look for gauge/metric fields that are set inside a helper method but the method is not called on every exit path of the encompassing function. Any early-return that skips a metric-update call leaves observability gaps that are invisible at runtime until an operator notices the gauge is perpetually zero.

**Test**: `RemoteLogManagerTest` was extended with cases that verify `RetentionSizeInPercent` and `LocalRetentionSizeInPercent` report correct non-zero values when no remote segments exist, when total size is within retention, and when total size exceeds retention. Assertions compare the percentage integer returned by `retentionSizeInPercent()` / `localRetentionSizeInPercent()` against expected computed values.
