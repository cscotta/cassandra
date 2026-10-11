**Commit**: 18199028a6 · **Ticket**: KAFKA-17995
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
If a user has configured value of `retention.ms` to a value greater than current unix timestamp epoch, then we fail cleanup of a remote log segment with an error. This change fixes the bug by handling this case of large `retention.ms` correctly. Reviewers: Divij Vaidya <diviv@amazon.com>

**Before/After**
```java
// Before
            return retentionMs > -1
                    ? Optional.of(new RetentionTimeData(retentionMs, time.milliseconds() - retentionMs))

// After
            long cleanupUntilMs = time.milliseconds() - retentionMs;
            return retentionMs > -1 && cleanupUntilMs >= 0
                    ? Optional.of(new RetentionTimeData(retentionMs, cleanupUntilMs))
    @Test
    public void testDeleteRetentionMsBiggerThanTimeMs() throws RemoteStorageException, ExecutionException, InterruptedException {
        // add 1 month to the current time to avoid flaky test
        LogConfig mockLogConfig = new LogConfig(Map.of("retention.ms", time.milliseconds() + 24 * 30 * 60 * 60 * 1000L));
        when(mockLog.config()).thenReturn(mockLogConfig);

        RemoteLogManager.RLMExpirationTask leaderTask = remoteLogManager.new RLMExpirationTask(leaderTopicIdPartition);

        when(mockLog.topicPartition()).thenReturn(leaderTopicIdPartition.topicPartition());
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17995: fix errors in remote segment cleanup when retention.ms is large (#17794).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
