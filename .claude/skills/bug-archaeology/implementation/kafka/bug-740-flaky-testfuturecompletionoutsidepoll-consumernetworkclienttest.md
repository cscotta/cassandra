**Commit**: 8036e49a6e · **Ticket**: KAFKA-17554
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Jira: https://issues.apache.org/jira/browse/KAFKA-17554

In the previous workflow, the test passes under two conditions:

1. The `t1` thread is waiting for the main thread's `client.wakeup()`. If successful, `t1` will wake up `t2`, allowing `t2` to complete the
future. 2.

**Before/After**
```java
// Before
import org.junit.jupiter.api.Disabled;
    @Disabled("KAFKA-17554")

        final CountDownLatch t1TheardCountDownLatch = new CountDownLatch(1);
        final CountDownLatch t2ThreadCountDownLatch = new CountDownLatch(2);

        Thread t1 = new Thread(() ->  {
            t1TheardCountDownLatch.countDown();
            t2ThreadCountDownLatch.countDown();

        t1.start();
            try {

// After
        CountDownLatch bothThreadsReady = new CountDownLatch(2);


        Thread t1 = new Thread(() -> {
            bothThreadsReady.countDown();
            bothThreadsReady.countDown();
            consumerClient.poll(future);

        t1.start();

        // Wait until both threads are blocked in poll
        bothThreadsReady.await();
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17554 flaky testfuturecompletionoutsidepoll in consumernetworkclienttest (#18298).

**Test**
Test modified in ConsumerNetworkClientTest.java covering the corrected behaviour.
