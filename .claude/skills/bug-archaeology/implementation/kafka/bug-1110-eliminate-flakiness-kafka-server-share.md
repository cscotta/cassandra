**Commit**: caf4a6cc5f · **Ticket**: KAFKA-19216
**Subsystem**: coordinator-common/src
**Category**: Logic error in condition

**Root cause**
### About
11 of the test cases in `SharePartitionTest` have failed at least once
in the past 28 days. https://develocity.apache.org/scans/tests?search.relativeStartTime=P28D&search.rootProjectNames=kafka&search.timeZoneId=Europe%2FLondon&tests.container=kafka.server.share.SharePartitionTest
Observing the flakiness, they seem to be caused due to the usage of
`SystemTimer` for various acquisition lock timeout related tests. I have
replaced the usage of `SystemTimer` with `MockTimer` and also improved
the `MockTimer` API with regard to removing the timer task entries that
have already been cancelled. Also, this has reduced the time taken to run `SharePartitionTest` from
~6 sec to ~1.5 sec

### ...

**Before/After**
```java
// Before
        // All timer tasks have been cancelled. TimerTask entries are not removed in MockTimer.
        assertEquals(2, timer.size());
        timer.taskQueue().forEach(taskEntry -> assertTrue(taskEntry.cancelled()));
        // All timer tasks have been cancelled. TimerTask entries are not removed in MockTimer.
        assertEquals(1, timer.size());
        timer.taskQueue().forEach(taskEntry -> assertTrue(taskEntry.cancelled()));
import org.apache.kafka.server.util.timer.SystemTimer;
import org.apache.kafka.server.util.timer.SystemTimerReaper;
    private static final int DEFAULT_MAX_WAIT_ACQUISITION_LOCK_TIMEOUT_MS = 300;
        mockTimer = new SystemTimerReaper("share-group-lock-timeout-test-reaper",
            new SystemTimer("share-group-lock-test-timeout"));

// After
        // All timer tasks have been cancelled. Hence,they have been removed in MockTimer.
        assertEquals(0, timer.size());
        // All timer tasks have been cancelled. Hence, they have been removed in MockTimer.
        assertEquals(0, timer.size());
import org.apache.kafka.server.util.timer.MockTimer;
    private static final int DEFAULT_MAX_WAIT_ACQUISITION_LOCK_TIMEOUT_MS = 120;
        mockTimer = new MockTimer();
        mockTimer.advanceClock(DEFAULT_MAX_WAIT_ACQUISITION_LOCK_TIMEOUT_MS);
        mockTimer.advanceClock(DEFAULT_MAX_WAIT_ACQUISITION_LOCK_TIMEOUT_MS);
        mockTimer.advanceClock(DEFAULT_MAX_WAIT_ACQUISITION_LOCK_TIMEOUT_MS);
```

**How to spot**
When reviewing coordinator-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19216: eliminate flakiness in kafka.server.share.sharepartitiontest (#19639).

**Test**
Test modified in CoordinatorRuntimeTest.java covering the corrected behaviour.
