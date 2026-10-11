**Commit**: 4271565409 · **Ticket**: KAFKA-16900
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Currently, users need to set --transaction-duration-ms to enable transactions in kafka-producer-perf-test, which is not straightforward. A better approach is to enable transactions when a transaction ID is provided. This PR allows enabling transaction in kafka-producer-perf-test by either

- set transaction.id=<id> via --producer-props or
- set transaction.id=<id> in config file via --producer.config or
- set --transaction-id <id> or
- set --transaction-duration-ms=<ms>

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before

            Namespace res = parser.parseArgs(args);

            /* parse args */
            String topicName = res.getString("topic");
            long numRecords = res.getLong("numRecords");
            Integer recordSize = res.getInt("recordSize");
            double throughput = res.getDouble("throughput");
            boolean payloadMonotonic = res.getBoolean("payloadMonotonic");
            List<String> producerProps = res.getList("producerConfig");
            String producerConfig = res.getString("producerConfigFile");
            String payloadFilePath = res.getString("payloadFile");

// After
import org.apache.kafka.common.Uuid;
import java.util.Optional;
    public static final String DEFAULT_TRANSACTION_ID_PREFIX = "performance-producer-";
    public static final long DEFAULT_TRANSACTION_DURATION_MS = 3000L;


            ConfigPostProcessor config = new ConfigPostProcessor(parser, args);
            KafkaProducer<byte[], byte[]> producer = createKafkaProducer(config.producerProps);
            if (config.transactionsEnabled)
            if (config.recordSize != null) {
                payload = new byte[config.recordSize];
            // not thread-safe, do not share with other threads
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16900 kafka-producer-perf-test reports error when using transaction. (#16646).

**Test**
Test modified in ProducerPerformanceTest.java covering the corrected behaviour.
