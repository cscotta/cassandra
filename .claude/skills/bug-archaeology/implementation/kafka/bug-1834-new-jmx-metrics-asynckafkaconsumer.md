**Commit**: 770d64d2cc · **Ticket**: KAFKA-16143
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Kirk True <ktrue@confluent.io>, Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.clients.consumer.internals.metrics.KafkaConsumerMetrics;
import java.util.LinkedList;
    private final KafkaConsumerMetrics kafkaConsumerMetrics;
            final BackgroundEventHandler backgroundEventHandler = new BackgroundEventHandler(backgroundEventQueue);
                    false);
                    requestManagersSupplier);
            this.kafkaConsumerMetrics = new KafkaConsumerMetrics(metrics, CONSUMER_METRIC_GROUP_PREFIX);

        this.kafkaConsumerMetrics = new KafkaConsumerMetrics(me

// After
import org.apache.kafka.clients.consumer.internals.metrics.AsyncConsumerMetrics;
    private final AsyncConsumerMetrics kafkaConsumerMetrics;
    private final BackgroundEventHandler backgroundEventHandler;
            this.kafkaConsumerMetrics = new AsyncConsumerMetrics(metrics);
            this.backgroundEventHandler = new BackgroundEventHandler(
                backgroundEventQueue,
                time,
                kafkaConsumerMetrics
            );
                    false,
                    kafkaConsumerMetrics
            );
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16143: new jmx metrics for asynckafkaconsumer (#17199).

**Test**
Test modified in ApplicationEventHandlerTest.java covering the corrected behaviour.
