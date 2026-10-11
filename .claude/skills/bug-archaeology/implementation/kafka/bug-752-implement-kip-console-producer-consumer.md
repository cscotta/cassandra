**Commit**: 3bc50f937c · **Ticket**: KAFKA-19623
**Subsystem**: docker/examples
**Category**: Logic error in condition

**Root cause**
*What*
https://issues.apache.org/jira/browse/KAFKA-19623

- The PR implements KIP-1147

(https://cwiki.apache.org/confluence/display/KAFKA/KIP-1147%3A+Improve+consistency+of+command-line+arguments)
for the console tools i.e. `ConsoleProducer`, `ConsoleConsumer` and
`ConsoleShareConsumer`. - Currently the previous names for the options are still usable but
there will be warning message stating those are deprecated and will be
removed in a future version. - I have added unit tests and also manually verified using the console
tools that things are working as expected.

**Before/After**
```java
// Before
    $ bin/kafka-console-producer.sh --topic test --bootstrap-server localhost:9093 --producer.config ./docker/examples/fixtures/client-secrets/client-ssl.properties
    $ bin/kafka-console-producer.sh --topic test --bootstrap-server localhost:9093 --producer.config ./docker/examples/fixtures/client-secrets/client-ssl.properties
        $ bin/kafka-console-producer.sh --topic test --bootstrap-server localhost:29093 --producer.config ./docker/examples/fixtures/client-secrets/client-ssl.properties
        $ bin/kafka-console-producer.sh --topic test --bootstrap-server localhost:29093 --producer.config ./docker/examples/fixtures/client-secrets/client-ssl.properties
        command = [f"{self.FIXTURES_DIR}/{constants.KAFKA_CONSOLE_CONSUMER}", "--topic", topic, "--property", "'print.key=true'", "--property", "'key.separator=:'", "--from-beginning", "--max-messages", "1", "--timeout-ms", f"{constants.CLIENT_TIMEOUT}"]
        producer_config = ["--bootstrap-server", "localhost:9092", "--property", "client.id=host"]
        consumer_config = ["--bootstrap-server", "localhost:9092", "--property", "auto.offset.reset=earliest"]
                           "--producer.config", f"{self.FIXTURES_DIR}/{constants.SSL_CLIENT_CONFIG}"]

// After
    $ bin/kafka-console-producer.sh --topic test --bootstrap-server localhost:9093 --command-config ./docker/examples/fixtures/client-secrets/client-ssl.properties
    $ bin/kafka-console-producer.sh --topic test --bootstrap-server localhost:9093 --command-config ./docker/examples/fixtures/client-secrets/client-ssl.properties
        $ bin/kafka-console-producer.sh --topic test --bootstrap-server localhost:29093 --command-config ./docker/examples/fixtures/client-secrets/client-ssl.properties
        $ bin/kafka-console-producer.sh --topic test --bootstrap-server localhost:29093 --command-config ./docker/examples/fixtures/client-secrets/client-ssl.properties
        command = [f"{self.FIXTURES_DIR}/{constants.KAFKA_CONSOLE_CONSUMER}", "--topic", topic, "--formatter-property", "'print.key=true'", "--formatter-property", "'key.separator=:'", "--from-beginning", "--max-messages", "1", "--timeout-ms", f"{constants.CLIENT_TIMEOUT}"]
        producer_config = ["--bootstrap-server", "localhost:9092", "--command-property", "client.id=host"]
        consumer_config = ["--bootstrap-server", "localhost:9092", "--command-property", "auto.offset.reset=earliest"]
                           "--command-config", f"{self.FIXTURES_DIR}/{constants.SSL_CLIENT_CONFIG}"]
```

**How to spot**
When reviewing docker/examples code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19623: implement kip-1147 for console producer/consumer/share-consumer. (#20479).

**Test**
Test modified in docker_sanity_test.py covering the corrected behaviour.
