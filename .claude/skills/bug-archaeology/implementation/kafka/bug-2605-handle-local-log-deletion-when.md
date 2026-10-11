**Commit**: 164f899605 · **Ticket**: KAFKA-17236
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Handle local log deletion when remote.log.copy.disabled=true based on the KIP-950. When tiered storage is disabled or becomes read-only on a topic, the local retention configuration becomes irrelevant, and all data expiration follows the topic-wide retention configuration exclusively. - added remoteLogEnabledAndRemoteCopyEnabled method to check if this topic enables tiered storage and remote log copy is enabled. We should adopt local.retention.ms/bytes when remote.storage.enable=true,remote.log.copy.disable=false.

**Before/After**
```java
// Before
            " and no more data uploading on a topic.";
      if (remoteLogEnabled()) {
    if (remoteLogEnabled()) maybeIncrementLocalLogStartOffset(startOffset, reason)
    val retentionMs = localRetentionMs(config, remoteLogEnabled())
    val retentionSize: Long = localRetentionSize(config, remoteLogEnabled())
  private[log] def localRetentionMs(config: LogConfig, remoteLogEnabled: Boolean): Long = {
    if (re

// After
            " and no more data uploading on a topic. Once this config is set to true, the local retention configuration " +
            "(i.e. local.retention.ms/bytes) becomes irrelevant, and all data expiration follows the topic-wide retention configuration" +
            "(i.e. retention.ms/bytes).";
    LogConfig.validateRetentionConfigsWhenRemoteCopyDisabled(newLogConfig.values(), isRemoteLogStorageEnabled)
  /**
   * @return true if this topic enables tiered storage and remote log copy is enabled (i.e. remote.log.copy.disable=false)
   */
  private def remoteLogEnabledAndRemoteCopyEnabled(): Boolean = {
    remoteLogEnabled() && !config.remoteLogCopyDisable()
  }

      // Note: when remote log copy is disabled, we will fall back to local log check using retention.ms/bytes
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17236: handle local log deletion when remote.log.copy.disabled=true (#16765).

**Test**
Test modified in RemoteTopicCrudTest.scala covering the corrected behaviour.
