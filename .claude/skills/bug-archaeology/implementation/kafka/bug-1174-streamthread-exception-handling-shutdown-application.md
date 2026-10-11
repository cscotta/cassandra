**Commit**: 144101a7c1 · **Ticket**: KAFKA-19054
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Under the `SHUTDOWN_APPLICATION` configuration in Kafka Streams, a tight
loop in the shutdown process can flood logs with repeated messages. This
PR introduces a check to ensure that the shutdown log is emitted only
once every 10 seconds, thereby preventing log flooding. Reviewers: PoAn Yang <payang@apache.org>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before

        try (final KafkaStreams kafkaStreams1 = new KafkaStreams(topology, properties);
             final KafkaStreams kafkaStreams2 = new KafkaStreams(topology, properties)) {
            log.warn("Detected that shutdown was requested. " +
                    "All clients in this app will now begin to shutdown");

// After
import org.apache.kafka.common.utils.LogCaptureAppender;
import org.apache.kafka.common.utils.MockTime;
import org.apache.kafka.streams.processor.internals.StreamThread;
import org.apache.logging.log4j.Level;
        final MockTime time = new MockTime(0L);

        try (final KafkaStreams kafkaStreams1 = new KafkaStreams(topology, properties, time);
             final KafkaStreams kafkaStreams2 = new KafkaStreams(topology, properties, time);
             final LogCaptureAppender logCaptureAppender = LogCaptureAppender.createAndRegister()) {
            logCaptureAppender.setClassLogger(StreamThread.class, Level.WARN);
            assertThat("Shutdown warning log message should be exported exactly once",
                    logCaptureAppender.getMessages("WARN").stream().filter(msg -> msg.contains("Detected that shutdown was requested")).count(), equalTo(1L));
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19054: streamthread exception handling with shutdown_application may trigger a tight loop with many logs (#19394).

**Test**
Test modified in StreamsUncaughtExceptionHandlerIntegrationTest.java covering the corrected behaviour.
