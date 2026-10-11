**Commit**: 31d395163e · **Ticket**: KAFKA-17553
**Subsystem**: streams/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                    taskManager.awaitProcessableTasks();
    public void awaitProcessableTasks() throws InterruptedException {
                log.debug("Await blocking");
                tasksCondition.await();
    void awaitProcessableTasks() throws InterruptedException;
import org.junit.jupiter.api.Timeout;
@Timeout(300)
        verify(taskManager, timeout(VERIFICATION_TIMEOUT).atLeastOnce()).awaitProcessableTasks();

// After
                    taskManager.awaitProcessableTasks(shutdownRequested::get);
    public void awaitProcessableTasks(final Supplier<Boolean> isShuttingDown) throws InterruptedException {
                // We re-check the shutdownRequest atomic boolean to avoid a race condition. If this thread was
                // previously interrupted while awaiting tasksCondition, it is possible to miss the signalAll that
                // is called during shutdown. If this happens, we end up blocking in the await forever.
                if (!isShuttingDown.get()) {
                    log.debug("Await blocking");
                    tasksCondition.await();
                } else {
                    log.debug("Not awaiting since shutdown was requested");
                }
import java.util.function.Supplier;
```

**How to spot**
When reviewing streams/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17553 fix shutdown race condition in streamthreadtest (#17191).

**Test**
Test modified in StreamThreadTest.java covering the corrected behaviour.
