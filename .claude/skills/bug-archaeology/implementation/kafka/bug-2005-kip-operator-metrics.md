**Commit**: 50c15b94c9 · **Ticket**: KAFKA-17561
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Implementation of KIP-1091 adding operator metrics to Kafka Streams
Updated existing tests to validate added metrics
Reviewers: Bruno Cadonna <cadonna@apache.org>, Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
                    }).sorted().collect(Collectors.toList());
            final List<String> expectedInstanceMetrics = Arrays.asList("org.apache.kafka.stream.alive.stream.threads", "org.apache.kafka.stream.failed.stream.threads");

// After
                    }).filter(name -> !name.equals("org.apache.kafka.stream.thread.state"))// telemetry reporter filters out string metrics
                    .sorted().collect(Collectors.toList());
            final List<String> expectedInstanceMetrics = Arrays.asList(
                "org.apache.kafka.stream.alive.stream.threads",
                "org.apache.kafka.stream.client.state",
                "org.apache.kafka.stream.failed.stream.threads",
                "org.apache.kafka.stream.recording.level");

    private static final String CLIENT_STATE = "client-state";
    private static final String RECORDING_LEVEL = "recording-level";
    private static final String THREAD_STATE = "thread-state";
    private static final String THREAD_STATE_JMX = "state";
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17561: kip-1091 add operator metrics (#17820).

**Test**
Test modified in KafkaStreamsTelemetryIntegrationTest.java covering the corrected behaviour.
