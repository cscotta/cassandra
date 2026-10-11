**Commit**: d226b43597 · **Ticket**: KAFKA-18220
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
*What*
https://issues.apache.org/jira/browse/KAFKA-18220

- Currently, `AsyncConsumerMetrics` extends `KafkaConsumerMetrics`, but
is being used both by `AsyncKafkaConsumer` and `ShareConsumerImpl`. - `ShareConsumerImpl` only needs the async consumer metrics(the metrics
associated with the new consumer threading model). - This needs to be fixed, we are unnecessarily having
`KafkaConsumerMetrics` as a parent class for `ShareConsumer` metrics. Fix :
- In this PR, we have removed the dependancy of `AsyncConsumerMetrics`
on `KafkaConsumerMetrics` and made it an independent class which both
`AsyncKafkaConsumer` and `ShareConsumerImpl` will use.

**Before/After**
```java
// Before
import static org.apache.kafka.clients.consumer.internals.ConsumerUtils.CONSUMER_METRIC_GROUP_PREFIX;
    private final AsyncConsumerMetrics kafkaConsumerMetrics;
            this.kafkaConsumerMetrics = new AsyncConsumerMetrics(metrics);
                kafkaConsumerMetrics
                    kafkaConsumerMetrics
                    kafkaConsumerMetrics

// After
import org.apache.kafka.clients.consumer.internals.metrics.KafkaConsumerMetrics;
import static org.apache.kafka.clients.consumer.internals.ConsumerUtils.CONSUMER_METRIC_GROUP;
    private final AsyncConsumerMetrics asyncConsumerMetrics;
    private final KafkaConsumerMetrics kafkaConsumerMetrics;
            this.asyncConsumerMetrics = new AsyncConsumerMetrics(metrics, CONSUMER_METRIC_GROUP);
            this.kafkaConsumerMetrics = new KafkaConsumerMetrics(metrics);
                asyncConsumerMetrics
                    asyncConsumerMetrics
                    asyncConsumerMetrics
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18220: refactor asyncconsumermetrics to not extend kafkaconsumermetrics (#20283).

**Test**
Test modified in ApplicationEventHandlerTest.java covering the corrected behaviour.
