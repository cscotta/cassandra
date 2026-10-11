**Commit**: 11966a209a · **Ticket**: KAFKA-17360
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
1) When the local.retention.ms/bytes is set to -2, we didn't replace it with the server-side retention.ms/bytes config, so the -2 local retention won't take effect. 2) When setting retention.ms/bytes to -2, we can notice this log message:

```
Deleting segment LogSegment(baseOffset=10045, size=1037087, lastModifiedTime=1724040653922, largestRecordTimestamp=1724040653835) due to local log retention size -2 breach. Local log size after deletion will be 13435280. (kafka.log.UnifiedLog) [kafka-scheduler-6]
```
This is not helpful for users.

**Before/After**
```java
// Before
    deleteOldSegments(shouldDelete, RetentionMsBreach(this, remoteLogEnabled()))
    deleteOldSegments(shouldDelete, RetentionSizeBreach(this, remoteLogEnabled()))
    if (remoteLogEnabledAndRemoteCopyEnabled) config.remoteLogConfig.localRetentionMs else config.retentionMs
    if (remoteLogEnabledAndRemoteCopyEnabled) config.remoteLogConfig.localRetentionBytes else config.retentionSize
case class RetentionMsBreach(log: UnifiedLog, remoteLogEnabled: Boolean) extends SegmentDeletionReason {
    val retentionMs = UnifiedLog.localRetentionMs(log.config, remoteLogEnabled)
        if (remoteLogEnabled)
        if (remoteLogEnabled)
case class RetentionSizeBreach(log: UnifiedLog, remoteLogEnabled: Boolean) extends SegmentDeletionReason {
      if (remoteLogEnabled) log.info(s"Deleting segment $segment due to local log retention size ${UnifiedLog.localRetentionSize(log.config, remoteLogEnabled)} breach. " +

// After
    deleteOldSegments(shouldDelete, RetentionMsBreach(this, remoteLogEnabledAndRemoteCopyEnabled()))
    deleteOldSegments(shouldDelete, RetentionSizeBreach(this, remoteLogEnabledAndRemoteCopyEnabled()))
    if (remoteLogEnabledAndRemoteCopyEnabled) config.localRetentionMs else config.retentionMs
    if (remoteLogEnabledAndRemoteCopyEnabled) config.localRetentionBytes else config.retentionSize
case class RetentionMsBreach(log: UnifiedLog, remoteLogEnabledAndRemoteCopyEnabled: Boolean) extends SegmentDeletionReason {
    val retentionMs = UnifiedLog.localRetentionMs(log.config, remoteLogEnabledAndRemoteCopyEnabled)
        if (remoteLogEnabledAndRemoteCopyEnabled)
        if (remoteLogEnabledAndRemoteCopyEnabled)
case class RetentionSizeBreach(log: UnifiedLog, remoteLogEnabledAndRemoteCopyEnabled: Boolean) extends SegmentDeletionReason {
      if (remoteLogEnabledAndRemoteCopyEnabled) log.info(s"Deleting segment $segment due to local log retention size ${UnifiedLog.localRetentionSize(log.config, remoteLogEnabledAndRemoteCopyEnabled)} breach. " +
  @Test
  def testRetentionOnLocalLogDeletionWhenRemoteLogCopyEnabledAndDefaultLocalRetentionBytes(): Unit = {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17360 local log retention ms/bytes "-2" is not treated correctly (#16932).

**Test**
Test modified in UnifiedLogTest.scala covering the corrected behaviour.
