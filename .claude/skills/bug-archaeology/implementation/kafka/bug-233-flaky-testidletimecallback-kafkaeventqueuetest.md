**Commit**: a0446e3333 · **Ticket**: KAFKA-20363
**Subsystem**: server-common/src
**Category**: Race condition / missing synchronization

**Root cause**
https://issues.apache.org/jira/browse/KAFKA-20363

The test had a race condition between Test thread and Queue thread. We
actually wait until queue thread is sleeping/waiting so it guarantees
the order. Reviewers: junvelop <151982401+junjunclub@users.noreply.github.com>,
 TaiJuWu <tjwu1217@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    /**
     * Wait for the queue's event handler thread to enter cond.await(), ensuring
     * that startIdleMs has been captured before the test advances MockTime.
     */
    private static void waitForQueueThreadToBeIdle(Thread queueThread) throws InterruptedException {
        TestUtils.waitForCondition(
                () -> queueThread.getState() == Thread.State.WAITING,
                "Queue thread should be waiting"
        );
    }

            // Capture the queue's event handler thread so we can wait for it to be idle.
```

**How to spot**
When reviewing server-common/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20363 fix flaky testidletimecallback in kafkaeventqueuetest (#21920).

**Test**
Test modified in KafkaEventQueueTest.java covering the corrected behaviour.
