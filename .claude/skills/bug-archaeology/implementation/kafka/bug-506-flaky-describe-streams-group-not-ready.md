## Bug 506 — Flaky test: testDescribeStreamsGroupsNotReady annotated @Flaky

**Commit:** e7d727e66e
**Ticket:** KAFKA-20045 (MINOR)
**Subsystem:** core / admin integration tests
**Category:** Race condition / missing synchronization

### Root cause
`PlaintextAdminIntegrationTest#testDescribeStreamsGroupsNotReady` races against the group coordinator: by the time the assertion runs, the streams group may already have transitioned out of `EMPTY` state, causing the test to see a different group state. The test was not designed with sufficient wait conditions to tolerate the asynchronous nature of streams group bootstrapping.

### Before
Test ran without any flaky annotation; intermittent CI failures.

### After
```java
@Flaky("KAFKA-20045")
@Test
def testDescribeStreamsGroupsNotReady(): Unit = { … }
```

### How to spot
Integration tests that assert on coordinator state (group state, member counts, offsets) immediately after triggering an asynchronous group event, without a `TestUtils.waitUntilTrue` or similar polling loop, are inherently race-prone.

### Test
Annotation prevents CI build failure on flap; root-cause fix tracked in KAFKA-20045.
