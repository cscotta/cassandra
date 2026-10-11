**Commit**: 3ec8563220 · **Ticket**: KAFKA-20272
**Subsystem**: group-coordinator/src/test/java/org/apache/kafka/coordinator/group/GroupMetadataManagerTest.java
**Category**: Wrong constant / default value

**Root cause**: KAFKA-20066 added `assignmentEpoch` fields to member assignments in the group coordinator. Tests added by KAFKA-20272 and KAFKA-20273 used the bare `mkAssignment(...)` helper that produces assignments without epoch annotations, causing assertion failures once the model required epochs. The fix wraps all affected `mkAssignment` calls with `toAssignmentWithEpochs(..., 10)`.

**Before/After**:
```java
// BEFORE – missing epoch annotation on assignments
.setAssignedPartitions(mkAssignment(
    mkTopicAssignment(fooTopicId, 0, 1, 2),
    mkTopicAssignment(barTopicId, 0, 1)))

// AFTER – epoch added
.setAssignedPartitions(toAssignmentWithEpochs(mkAssignment(
    mkTopicAssignment(fooTopicId, 0, 1, 2),
    mkTopicAssignment(barTopicId, 0, 1)), 10))
```

**How to spot**: When a data model gains a new required field, grep all test helpers (e.g., `mkAssignment`) for usages — any call site that does not pass through the new wrapper will silently produce an incomplete object. This is structurally identical to bug-190; both are merge-conflict test fixes in the same test file.

**Test**: Multiple consumer group member-builder test cases in `GroupMetadataManagerTest` are updated. No new scenario added; all changes correct assignment epoch values to satisfy post-KAFKA-20066 invariants.
