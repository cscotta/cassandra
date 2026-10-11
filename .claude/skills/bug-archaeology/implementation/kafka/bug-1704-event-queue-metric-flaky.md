**Commit**: 0721d21a57 · **Ticket**: KAFKA-18415
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
        asyncConsumerMetrics.recordApplicationEventQueueSize(applicationEventQueue.size());
        asyncConsumerMetrics.recordBackgroundEventQueueSize(backgroundEventQueue.size());
import static org.junit.jupiter.api.Assertions.assertEquals;
             AsyncConsumerMetrics asyncConsumerMetrics = new AsyncConsumerMetrics(metrics);
            assertEquals(
                1,
                (double) metrics.metric(
                    metrics.metricName(
                        AsyncConsumerMetrics.APPLICATION_EVENT_QUEUE_SIZE_SENSOR_NAME,
                        ConsumerUtils.CONSUMER_METRIC_GROUP
                    )
                ).metricValue()

// After
        // Record the updated queue size before actually adding the event to the queue
        // to avoid race conditions (the background thread is continuously removing from this queue)
        asyncConsumerMetrics.recordApplicationEventQueueSize(applicationEventQueue.size() + 1);
        asyncConsumerMetrics.recordBackgroundEventQueueSize(backgroundEventQueue.size() + 1);
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
             AsyncConsumerMetrics asyncConsumerMetrics = spy(new AsyncConsumerMetrics(metrics));
            verify(asyncConsumerMetrics).recordApplicationEventQueueSize(1);
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18415: fix for event queue metric and flaky test (#18416).

**Test**
Test modified in ApplicationEventHandlerTest.java covering the corrected behaviour.
