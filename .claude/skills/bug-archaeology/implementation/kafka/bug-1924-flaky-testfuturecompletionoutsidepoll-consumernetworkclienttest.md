**Commit**: ff44f5e0a5 · **Ticket**: KAFKA-17554
**Subsystem**: clients/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        Thread t1 = new Thread(() -> consumerClient.pollNoWakeup());
        // Sleep a little so that t1 is blocking in poll
        Thread.sleep(50);

        Thread t2 = new Thread(() -> consumerClient.poll(future));

        // Sleep a little so that t2 is awaiting the network client lock
        Thread.sleep(50);


// After
import java.util.concurrent.CountDownLatch;

        final CountDownLatch t1TheardCountDownLatch = new CountDownLatch(1);
        final CountDownLatch t2ThreadCountDownLatch = new CountDownLatch(2);
        Thread t1 = new Thread(() ->  {
            t1TheardCountDownLatch.countDown();
            consumerClient.pollNoWakeup();
            t2ThreadCountDownLatch.countDown();
        });

        Thread t2 = new Thread(() -> {
            try {
```

**How to spot**
When reviewing clients/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17554 flaky testfuturecompletionoutsidepoll in consumernetworkclienttest (#17217).

**Test**
Test modified in ConsumerNetworkClientTest.java covering the corrected behaviour.
