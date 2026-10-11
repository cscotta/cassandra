**Commit**: b015a83f6d · **Ticket**: KAFKA-17017
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lianet Magrans <lianetmr@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            if (groupMetadata.get().isPresent()) {
                Timer timer = time.timer(Long.MAX_VALUE);
                UnsubscribeEvent unsubscribeEvent = new UnsubscribeEvent(calculateDeadlineMs(timer));
                applicationEventHandler.add(unsubscribeEvent);
                log.info("Unsubscribing all topics or patterns and assigned partitions {}",
                try {
                    processBackgroundEvents(unsubscribeEvent.future(), timer);
                    log.info("Unsubscribed all topics or patterns and assigned partitions");
                } catch (TimeoutException e) {
                    log.error("Failed while waiting for the unsubscribe event to complete");
                }
                resetGroupMetadata();

// After
                    subscriptions,
                subscriptions,
            Timer timer = time.timer(Long.MAX_VALUE);
            UnsubscribeEvent unsubscribeEvent = new UnsubscribeEvent(calculateDeadlineMs(timer));
            applicationEventHandler.add(unsubscribeEvent);
            log.info("Unsubscribing all topics or patterns and assigned partitions {}",
            try {
                processBackgroundEvents(unsubscribeEvent.future(), timer);
                log.info("Unsubscribed all topics or patterns and assigned partitions");
            } catch (TimeoutException e) {
                log.error("Failed while waiting for the unsubscribe event to complete");
            resetGroupMetadata();
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17017 asynckafkaconsumer#unsubscribe clean the assigned partitions (#16449).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
