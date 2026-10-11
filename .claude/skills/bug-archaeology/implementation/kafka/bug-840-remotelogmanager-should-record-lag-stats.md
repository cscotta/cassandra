**Commit**: 96c8e86cdf · **Ticket**: KAFKA-19530
**Subsystem**: storage/src
**Category**: Logic error in condition

**Root cause**
When remote storage is offline, then the segmentLag and bytesLag metrics
are not recorded. These metrics are useful to know the pending data to
upload when remote storage is down. Reviewers: TaiJuWu <tjwu1217@gmail.com>, Kamal Chandraprakash
 <kamal.chandraprakash@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
                            } catch (Exception e) {
                                recordLagStats(log);
                                throw e;
            recordLagStats(log);
        }

        private void recordLagStats(UnifiedLog log) {
        when(mockLog.onlyLocalLogSegmentsSize()).thenReturn(12L);
        when(mockLog.onlyLocalLogSegmentsCount()).thenReturn(2L);
        when(activeSegment.size()).thenReturn(2);
        assertEquals(10, brokerTopicStats.allTopicsStats().remoteCopyLagBytesAggrMetric().value());
        assertEquals(1, brokerTopicStats.allTopicsStats().remoteCopyLagSegmentsAggrMetric().value());
```

**How to spot**
When reviewing storage/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19530 remotelogmanager should record lag stats when remote storage is offline (#20218).

**Test**
Test modified in RemoteLogManagerTest.java covering the corrected behaviour.
