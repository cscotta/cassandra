## Bug 514 — Flaky test: MonitorableSinkIntegrationTest timing issue

**Commit:** 2bae9c36f2
**Ticket:** KAFKA-18952
**Subsystem:** connect / sink integration tests
**Category:** Race condition / missing synchronization

### Root cause
`MonitorableSinkIntegrationTest` asserted on sink task metrics (records written, errors) without adequate waiting for the Connect worker to flush and commit the final batch. Under load the assertions fired before the worker had finished processing, producing intermittent failures. The fix introduced a proper `waitForCondition` loop instead of a fixed sleep, and ensured the sink connector had fully committed before asserting.

### Before
```java
Thread.sleep(2000); // arbitrary sleep — race-prone
assertEquals(expectedRecords, sinkMetrics.recordsWritten());
```

### After
```java
waitForCondition(
    () -> sinkMetrics.recordsWritten() == expectedRecords,
    "Sink did not write expected records"
);
```

### How to spot
Integration tests that use `Thread.sleep` before asserting on asynchronous state (offsets committed, records written, connector status) are race-prone. Replace with polling `waitForCondition`.

### Test
`MonitorableSinkIntegrationTest` passes deterministically after replacing sleep with condition wait.
