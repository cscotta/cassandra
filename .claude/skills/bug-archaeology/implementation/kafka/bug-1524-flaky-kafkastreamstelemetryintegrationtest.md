**Commit**: ee4f8f8c42 · **Ticket**: KAFKA-18541
**Subsystem**: streams/integration-tests
**Category**: Logic error in condition

**Root cause**
Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
            TestUtils.waitForCondition(() -> !TelemetryPlugin.SUBSCRIBED_METRICS.get(mainConsumerInstanceId).isEmpty(),
                    30_000,
                    "Never received subscribed metrics");
            TestUtils.waitForCondition(() -> !TelemetryPlugin.SUBSCRIBED_METRICS.get(adminInstanceId).isEmpty(),
                    30_000,
                    "Never received subscribed metrics");

                waitForCondition(() -> KafkaStreams.State.RUNNING == streamsTwo.state() && KafkaStreams.State.RUNNING == streamsOne.state(),
                        IntegrationTestUtils.DEFAULT_TIMEOUT,
                        () -> "Kafka Streams one or two never transitioned to a RUNNING state.");
                final List<String> streamOneTaskIds = getTaskIdsAsStrings(streamsOne);
                final List<String> streamTwoTasksIds = getTaskIdsAsStrings(streamsTwo);

// After
import java.util.Collections;
            TestUtils.waitForCondition(
                () -> !TelemetryPlugin.SUBSCRIBED_METRICS.getOrDefault(mainConsumerInstanceId, Collections.emptyList()).isEmpty(),
                30_000,
                "Never received subscribed metrics"
            );
            TestUtils.waitForCondition(
                () -> !TelemetryPlugin.SUBSCRIBED_METRICS.getOrDefault(adminInstanceId, Collections.emptyList()).isEmpty(),
                30_000,
                "Never received subscribed metrics"
            );
                final List<String> streamOneTaskIds = new ArrayList<>();
```

**How to spot**
When reviewing streams/integration-tests code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18541: fix flaky kafkastreamstelemetryintegrationtest (#18569).

**Test**
Test modified in KafkaStreamsTelemetryIntegrationTest.java covering the corrected behaviour.
