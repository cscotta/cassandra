**Commit**: 11c10fe4da · **Ticket**: KAFKA-16368
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Ismael Juma <ismael@juma.me.uk>, Divij Vaidya <diviv@amazon.com>

**Before/After**
```java
// Before
                                                 + "This <code>linger.ms</code> setting defaults to 0, which means we'll immediately send out a record even the accumulated "
                                                 + "batch size is under this <code>batch.size</code> setting.";
                                                + "specified time waiting for more records to show up. This setting defaults to 0 (i.e. no delay). Setting <code>" + LINGER_MS_CONFIG + "=5</code>, "
                                                + "for example, would have the effect of reducing the number of requests sent but would add up to 5ms of latency to records sent in the absence of load.";
                                .define(LINGER_MS_CONFIG, Type.LONG, 0, atLeast(0), Importance.MEDIUM, LINGER_MS_DOC)

// After
                                                 + "This <code>linger.ms</code> setting defaults to 5, which means the producer will wait for 5ms or until the record batch is "
                                                 + "of <code>batch.size</code>(whichever happens first) before sending the record batch. Note that broker backpressure can "
                                                 + " result in a higher effective linger time than this setting."
                                                 + "The default changed from 0 to 5 in Apache Kafka 4.0 as the efficiency gains from larger batches typically result in "
                                                 + "similar or lower producer latency despite the increased linger.";
                                                + "specified time waiting for more records to show up. This setting defaults to 5 (i.e. 5ms delay). Increasing <code>" + LINGER_MS_CONFIG + "=50</code>, "
                                                + "for example, would have the effect of reducing the number of requests sent but would add up to 50ms of latency to records sent in the absence of load."
                                                + "The default changed from 0 to 5 in Apache Kafka 4.0 as the efficiency gains from larger batches typically result in "
                                                + "similar or lower producer latency despite the increased linger.";
                                .define(LINGER_MS_CONFIG, Type.LONG, 5, atLeast(0), Importance.MEDIUM, LINGER_MS_DOC)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16368: update default linger.ms to 5ms for kip-1030 (#18080).

**Test**
Test modified in ProducerSendWhileDeletionTest.scala covering the corrected behaviour.
