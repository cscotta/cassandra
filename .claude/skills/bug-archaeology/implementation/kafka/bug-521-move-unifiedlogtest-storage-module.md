**Commit**: 3a950f45ea · **Ticket**: KAFKA-19752
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
This PR migrates several unit tests from UnifiedLogTest.scala to
UnifiedLogTest.java as part of the ongoing effort to convert core broker
logic and tests to Java. The following test methods have been rewritten and moved:
- testDeleteOldSegments
- testLogDeletionAfterClose
- testLogDeletionAfterDeleteRecords
- shouldDeleteSizeBasedSegments
- shouldNotDeleteSizeBasedSegmentsWhenUnderRetentionSize
- shouldDeleteTimeBasedSegmentsReadyToBeDeleted
- shouldNotDeleteTimeBasedSegmentsWhenNoneReadyToBeDeleted
- shouldNotDeleteSegmentsWhenPolicyDoesNotIncludeDelete
- shouldDeleteSegmentsReadyToBeDeletedWhenCleanupPolicyIsCompactAndDelete
- shouldDeleteLocalLogSegmentsWhenPolicyIsEmptyWithSizeRetention
...

**Before/After**
```java
// Before

    log.deleteOldSegments()
    log.deleteOldSegments()
    log.deleteOldSegments()
    log.deleteOldSegments()
    log.deleteOldSegments() // force retention to kick in so that the snapshot files are cleaned up.
    log.deleteOldSegments()
  @Test
  def testDeleteOldSegments(): Unit = {
    def createRecords = TestUtils.singletonRecords(value = "test".getBytes, timestamp = mockTime.milliseconds - 1000)
    val logConfig = LogTestUtils.createLogConfig(segmentBytes = createRecords.sizeInBytes * 5, segmentIndexBytes = 1000, retentionMs = 99

// After
    assertTrue(log.deleteOldSegments > 0, "At least one segment should be deleted")
    assertTrue(log.deleteOldSegments > 0, "At least one segment should be deleted")
    // There is no other key so we don't delete anything
    assertEquals(0, log.deleteOldSegments())
    assertTrue(log.deleteOldSegments > 0, "At least one segment should be deleted")
    assertTrue(log.deleteOldSegments > 0, "At least one segment should be deleted")// force retention to kick in so that the snapshot files are cleaned up.
    assertTrue(log.deleteOldSegments > 0, "At least one segment should be deleted")
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19752 move unifiedlogtest to storage module (1/n) (#20635).

**Test**
Test modified in UnifiedLogTest.scala covering the corrected behaviour.
