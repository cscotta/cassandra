**Commit**: 20e101c2e4 · **Ticket**: KAFKA-16991
**Subsystem**: streams/src
**Category**: Race condition / missing synchronization

**Root cause**
When the PurgeRepartitionTopicintegrationTest was written, the InitialTaskDelayMs was hard-coded on the broker requiring setting a timeout in the test to wait for the delay to expire. But I believe this creates a race condition where the test times out before the broker deletes the inactive segment. PR #15719 introduced an internal config to control the IntitialTaskDelayMs config for speeding up tests, and this PR leverages this internal config to reduce the task delay to 0 to eliminate this race condition.

**Before/After**
```java
// Before

        // we need long enough timeout to by-pass the log manager's InitialTaskDelayMs, which is hard-coded on server side

// After
    private static final Integer INITIAL_TASK_DELAY_MS = 0;
            put("log.initial.task.delay.ms", INITIAL_TASK_DELAY_MS);

```

**How to spot**
When reviewing streams/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16991: flaky purgerepartitiontopicintegrationtest (#16503).

**Test**
Test modified in PurgeRepartitionTopicIntegrationTest.java covering the corrected behaviour.
