**Commit**: f48e764f7e · **Ticket**: KAFKA-17159
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                    executorService.shutdownNow();
                    executorService.awaitTermination(5, TimeUnit.MINUTES);
            executorService.shutdownNow();
            executorService.awaitTermination(5, TimeUnit.MINUTES);

// After
                    ThreadUtils.shutdownExecutorServiceQuietly(executorService, 5, TimeUnit.MINUTES);
            ThreadUtils.shutdownExecutorServiceQuietly(executorService, 5, TimeUnit.MINUTES);
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17159 make sure kafka-cluster-test-kit-executor get down when closing kafkaclustertestkit (#16633).

**Test**
Test modified in KafkaClusterTestKit.java covering the corrected behaviour.
