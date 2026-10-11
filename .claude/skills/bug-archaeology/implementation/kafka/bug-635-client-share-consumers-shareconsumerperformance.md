**Commit**: 907e56d265 · **Ticket**: KAFKA-19868
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
### About
In order to make observation easy, we've added different client id for
different share consumers (if threads is set to greater than 1) when
running `ShareConsumerPerformance` script. Screenshot with 3 share consumers -   <img width="864" height="288"
alt="Screenshot 2025-11-06 at 6 43 39 PM"

src="https://github.com/user-attachments/assets/039f1fcf-3385-4fbf-bb76-e637a3bd75b3"
/>

Screenshot with 1 share consumer -   <img width="862" height="152"
alt="Screenshot 2025-11-06 at 6 43 58 PM"

src="https://github.com/user-attachments/assets/21f52b03-4f3b-48bd-9a41-51ba58ec52fa"
/>

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                shareConsumers.add(shareConsumerCreator.apply(options.props()));
            consume(shareConsumers, options, totalRecordsRead, totalBytesRead, startMs);
            printStats(totalBytesRead.get(), totalRecordsRead.get(), elapsedSec, fetchTimeInMs, startMs, endMs,
                    options.dateFormat(), -1);
                                long startMs) throws ExecutionException, InterruptedException {
                printStats(bytesReadByConsumer, recordsReadByConsumer, elapsedSec, fetchTimeInMs, startMs, endMs, options.dateFormat(), index + 1);
    // Prints stats for both share consumer and share group. For share group, index is -1. For share consumer,
    // index is >= 1.
    private static void printStats(long bytesRead,
                                   long recordsRead,
                                   double elapsedSec,
                                   long fetchTimeInMs,

// After
            List<String> clientIds = new ArrayList<>();
                if (options.threads() == 1) {
                    clientIds.add(options.props().getProperty(ConsumerConfig.CLIENT_ID_CONFIG));
                    shareConsumers.add(shareConsumerCreator.apply(options.props()));
                    break;
                }
                Properties shareConsumerProps = options.props();
                String shareConsumerClientId = options.props().getProperty(ConsumerConfig.CLIENT_ID_CONFIG) + "-" + (i + 1);
                shareConsumerProps.put(ConsumerConfig.CLIENT_ID_CONFIG, shareConsumerClientId);
                clientIds.add(shareConsumerClientId);
                shareConsumers.add(shareConsumerCreator.apply(shareConsumerProps));
            consume(shareConsumers, options, totalRecordsRead, totalBytesRead, startMs, clientIds);
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19868 add client id to share consumers in shareconsumerperformance (#20840).

**Test**
No test added.
