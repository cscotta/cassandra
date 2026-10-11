**Commit**: 0eaaff88cf · **Ticket**: KAFKA-17151
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.test.TestUtils;
import java.util.concurrent.atomic.AtomicReference;
            ATTACH_LISTENER_THREAD_PREFIX, PROCESS_REAPER_THREAD_PREFIX, RMI_THREAD_PREFIX)
    public void afterEach(ExtensionContext context) throws InterruptedException {
        AtomicReference<List<Thread>> lastThread = new AtomicReference<>(Collections.emptyList());
        TestUtils.waitForCondition(() -> {
            List<Thread> threads = detectThreadLeak.newThreads();
            lastThread.set(threads);
            return threads.isEmpty();
        }, () -> "Thread leak detected: " +
                lastThread.get().stream().map(Thread::getName).collect(Collectors.joining(", ")));
            /*

// After
import org.apache.kafka.server.util.timer.SystemTimer;
import static org.junit.jupiter.api.Assertions.assertTrue;

            ATTACH_LISTENER_THREAD_PREFIX, PROCESS_REAPER_THREAD_PREFIX, RMI_THREAD_PREFIX, SystemTimer.SYSTEM_TIMER_THREAD_PREFIX)
    public void afterEach(ExtensionContext context) {
        List<Thread> threads = detectThreadLeak.newThreads();
        assertTrue(threads.isEmpty(), "Thread leak detected: " +
                threads.stream().map(Thread::getName).collect(Collectors.joining(", ")));
            return new KafkaClusterTestKit(
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17151 remove waitforcondition when detecting thread leak (#16661).

**Test**
Test modified in ClusterTestExtensions.java covering the corrected behaviour.
