**Commit**: f16d1f3c9d · **Ticket**: KAFKA-19299
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
This MR should be couple of race conditions in RemoteIndexCacheTest. 1. There was a race condition between cache-cleanup-thread and test
thread, which wants to check that cache is gone. This was fixed with
TestUtils#waitForCondition
2.

**Before/After**
```java
// Before
    public void detectLeaks() {
import java.util.stream.Collectors;
     * If any matching threads are found, the test will fail.
     * @throws AssertionError If any thread with the specified name prefix and daemon status is found and is alive.
    public static void assertNoLeakedThreadsWithNameAndDaemonStatus(String threadName, boolean isDaemon) {
        List<Thread> threads = Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.isDaemon() == isDaemon && t.isAlive() && t.getName().startsWith(threadName))
                .collect(Collectors.toList());
        int threadCount = threads.size();
        assertEquals(0, threadCount);

// After
    public void detectLeaks() throws InterruptedException {
     * Use the {@link #waitForCondition(TestCondition, String) waitForCondition} to retry the check at a regular interval
     * until either no matching threads are found or the timeout is exceeded.
     * If any matching, alive threads are found after the timeout has elapsed, the assertion will fail.
     * @throws AssertionError If any thread with the specified name prefix and daemon status are found after the timeout.
    public static void assertNoLeakedThreadsWithNameAndDaemonStatus(String threadName, boolean isDaemon) throws InterruptedException {
        waitForCondition(() -> Thread.getAllStackTraces().keySet().stream()
                .noneMatch(t -> t.isDaemon() == isDaemon && t.isAlive() && t.getName().startsWith(threadName)), String.format("Thread leak detected: %s", threadName));
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19299: fix race condition in remoteindexcachetest (#19927).

**Test**
Test modified in KafkaProducerTest.java covering the corrected behaviour.
