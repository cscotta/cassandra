**Commit**: ef10a52a52 · **Ticket**: KAFKA-19011
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
jira: https://issues.apache.org/jira/browse/KAFKA-19011  kip:

https://cwiki.apache.org/confluence/display/KAFKA/KIP-1172%3A+Improve+EndToEndLatency+tool

This PR improves the usability and maintainability of the
`kafka-e2e-latency.sh` tool:

- Replaces fixed-index argument parsing with a proper argument parser
(joptsimple)
- Adds support for configuring:
    - -record-key-size: size of the message key
    - -num-headers: number of headers per message
    - -record-header-key-size: size of each header key
    - -record-header-size: size of each header value
- Renames existing arguments to align with Kafka CLI conventions:
    - broker_list → bootstrap-server
    - num_messages → num-records
...

**Before/After**
```java
// Before
 * <p> broker_list = location of the bootstrap broker for both the producer and the consumer </p>
 * <p> topic = topic name used by both the producer and the consumer to send/receive messages </p>
 * <p> num_messages = # messages to send </p>
 * <p> producer_acks = See ProducerConfig.ACKS_DOC </p>
 * <p> message_size_bytes = size of each message in bytes </p>
 * <p> e.g. [localhost:9092 test 10000 1 20] </p>
    static void execute(String... args) throws Exception {
        if (args.length != 5 && args.length != 6) {
            throw new TerseException("USAGE: java " + EndToEndLatency.class.getName()
                    + " broker_list topic num_messages producer_acks message_size_bytes [optional] properties_file");
        }
        String brokers = args[0];

// After
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.server.util.CommandDefaultOptions;
import org.apache.kafka.server.util.CommandLineUtils;
import java.util.ArrayList;
import java.util.Iterator;
import joptsimple.OptionException;
import joptsimple.OptionSpec;

 * <p> --bootstrap-server = location of the bootstrap broker for both the producer and the consumer
 * <p> --topic = topic name used by both the producer and the consumer to send/receive messages
 * <p> --num-records = # messages to send
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19011 improve endtoendlatency tool with argument parser and message key/header support (#20301).

**Test**
Test modified in EndToEndLatencyTest.java covering the corrected behaviour.
