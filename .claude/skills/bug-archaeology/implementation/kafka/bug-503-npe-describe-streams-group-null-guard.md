## Bug 503 — NPE when firstGroup is null in testDescribeStreamsGroupsNotReady

**Commit:** 2585a9a7c4
**Ticket:** (MINOR marker — flaky fix for KAFKA-20045)
**Subsystem:** core / admin integration tests
**Category:** Missing null/bounds check

### Root cause
`PlaintextAdminIntegrationTest.testDescribeStreamsGroupsNotReady` called `findFirst().orElse(null)` on a stream of group results and then immediately dereferenced the result to check its `groupState()` field without guarding against `null`. When the group was not yet present the dereference threw a `NullPointerException`, making the test fail non-deterministically.

### Before
```scala
val firstGroup = groups.find(_.groupId() == streamsGroupId).orElse(null)
assertEquals(GroupState.EMPTY, firstGroup.groupState())  // NPE if null
```

### After
The test was annotated `@Flaky("KAFKA-20045")` while a proper fix is tracked. (The companion commit 2585a9a7c4 is the annotation commit.)

### How to spot
Calls to `Optional.findFirst().orElse(null)` (or Scala `.find().orNull`) where the result is used without a null guard immediately afterward are NPE time-bombs in flaky-test scenarios.

### Test
`PlaintextAdminIntegrationTest#testDescribeStreamsGroupsNotReady` marked `@Flaky` so CI no longer fails the build on this flap.
