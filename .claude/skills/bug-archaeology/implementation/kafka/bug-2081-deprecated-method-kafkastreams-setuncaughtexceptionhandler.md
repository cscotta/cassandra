**Commit**: c903bdf496 · **Ticket**: KAFKA-12827
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before

        streams.setUncaughtExceptionHandler((t, e) -> {

                streamThree,
                Arrays.asList(hello, hello, hello, hello, hello, hello, hello, hello),
                TestUtils.producerConfig(
                        CLUSTER.bootstrapServers(),
                        StringSerializer.class,
                        StringSerializer.class,
                        new Properties()),
                mockTime);
    @Deprecated //A single thread should no longer die

// After
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
        streams.setUncaughtExceptionHandler(e -> {
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
import static org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
            streamThree,
            Arrays.asList(hello, hello, hello, hello, hello, hello, hello, hello),
            TestUtils.producerConfig(
                CLUSTER.bootstrapServers(),
                StringSerializer.class,
                StringSerializer.class,
                new Properties()),
            mockTime);
```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12827 remove deprecated method kafkastreams#setuncaughtexceptionhandler (#16988).

**Test**
Test modified in EosIntegrationTest.java covering the corrected behaviour.
