**Commit**: c4dc78746e · **Ticket**: KAFKA-18537
**Subsystem**: storage/src
**Category**: Resource leak

**Root cause**
Add a wait for cleaner thread shutdown in `testCleanerThreadShutdown` to
eliminate flakiness. After calling `cache.close()`, the test now uses
`TestUtils.waitForCondition` to poll until the background
“remote-log-index-cleaner” thread has fully exited before asserting that
no cleaner threads remain. This ensures the asynchronous shutdown always
completes before the final assertions. Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
        threads = getRunningCleanerThread();
        assertTrue(threads.isEmpty(), "Found unexpected " + threads.size() + " threads=" + threads.stream().map(Thread::getName).collect(Collectors.joining(", ")));

// After
        TestUtils.waitForCondition(
                () -> getRunningCleanerThread().isEmpty(),
                () -> "Failed while waiting for cleaner threads to shutdown. Remaining threads: " +
                        getRunningCleanerThread().stream().map(Thread::getName).collect(Collectors.joining(", ")));
```

**How to spot**
When reviewing storage/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18537 fix flaky remoteindexcachetest#testcleanerthreadshutdown (#19628).

**Test**
Test modified in RemoteIndexCacheTest.java covering the corrected behaviour.
