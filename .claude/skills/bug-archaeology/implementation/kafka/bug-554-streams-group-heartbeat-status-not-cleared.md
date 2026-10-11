**Commit**: 963d54a06d · **Ticket**: KAFKA-19945

**Subsystem**: group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java

**Category**: Silent skip on non-match

**Root cause**
The `StreamsGroupHeartbeatResponse` JSON schema declared `"default": "null"` for the `Status` field, causing the serialised response to omit the field when the coordinator set it to `null`. Client code therefore never received a clearing signal when the status transitioned back to empty, leaving stale status information visible to the consumer. The fix removes the null default from the schema and changes `GroupMetadataManager` to unconditionally set the status field (with an empty list when there is nothing to report) rather than only setting it when non-empty.

**Before**
```java
// Before (schema had "default": "null")
if (!returnedStatus.isEmpty()) {
    response.setStatus(returnedStatus);
}
// Early-return path omitted .setStatus entirely
```

**After**
```java
// After
response.setStatus(returnedStatus); // always set, empty list when clear
// Early-return path
.setStatus(List.of())
```

**How to spot**
Look for protocol message JSON schemas that list `"default": "null"` on list-typed fields that should be explicitly cleared; any conditional set of such a field in the coordinator is a sign that null may silently suppress a clearing update. Cross-check that every code path that builds the response calls `setStatus`.

**Test**
Create a unit test where the coordinator transitions from a non-empty status to an empty status and verify the client receives an explicit empty list rather than a missing field.
