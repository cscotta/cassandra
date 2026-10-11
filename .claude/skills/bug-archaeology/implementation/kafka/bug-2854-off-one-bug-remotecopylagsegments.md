**Commit**: 817da3fb5d · **Ticket**: KAFKA-16895
**Subsystem**: core/src
**Category**: Off-by-one

**Root cause**
Reviewers: Kamal Chandraprakash <kchandraprakash@uber.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            long segmentsLag = log.onlyLocalLogSegmentsCount();
        when(mockLog.onlyLocalLogSegmentsCount()).thenReturn(1L);

// After
            long segmentsLag = log.onlyLocalLogSegmentsCount() - 1;
        when(mockLog.onlyLocalLogSegmentsCount()).thenReturn(2L).thenReturn(1L);

        TestUtils.waitForCondition(
                () -> 0 == safeLongYammerMetricValue("RemoteCopyLagBytes") && 0 == safeLongYammerMetricValue("RemoteCopyLagBytes,topic=" + leaderTopic),
                String.format("Expected to find 0 for RemoteCopyLagBytes metric value, but found %d for topic 'Leader' and %d for all topics.",
                        safeLongYammerMetricValue("RemoteCopyLagBytes,topic=" + leaderTopic),
                        safeLongYammerMetricValue("RemoteCopyLagBytes")));
        TestUtils.waitForCondition(
                () -> 0 == safeLongYammerMetricValue("RemoteCopyLagSegments") && 0 == safeLongYammerMetricValue("RemoteCopyLagSegments,topic=" + leaderTopic),
                String.format("Expected to find 0 for RemoteCopyLagSegments metric value, but found %d for topic 'Leader' and %d for all topics.",
                        safeLongYammerMetricValue("RemoteCopyLagSegments,topic=" + leaderTopic),
```

**How to spot**
When reviewing core/src code, look for off-by-one patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16895 fix off-by-one bug in remotecopylagsegments (#16210).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
