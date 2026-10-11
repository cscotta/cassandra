**Commit**: a931f85835 · **Ticket**: KAFKA-19625
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
As described in
[jira](https://issues.apache.org/jira/browse/KAFKA-19625), this PR
implements replace `consumer.config` and `producer.config` with
`command-config` for kafka-verifiable-producer.sh and
kafka-verifiable-consumer.sh. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
 * `--consumer.config <config-file>` - consumer config properties (typically empty)
 * `--producer.config <config-file>` - producer config properties (typically empty)
        cmd += " --consumer.config %s" % VerifiableConsumer.CONFIG_FILE
        cmd += " --producer.config %s" % VerifiableProducer.CONFIG_FILE
                .help("Set reset policy (must be either 'earliest', 'latest', or 'none'");
                .metavar("CONFIG_FILE")
                .help("Consumer config properties file (config options shared with command line parameters will be overridden).");

// After
 * `--consumer.config <config-file>` - (DEPRECATED) consumer config properties (typically empty). This option will be removed in a future version. Use --command-config instead.
 * `--command-config <config-file>` - command config properties
 * `--producer.config <config-file>` - producer config properties (typically empty). This option will be removed in a future version. Use --command-config instead.
 * `--command-config <config-file>` - command config properties
        cmd += " --command-config %s" % VerifiableConsumer.CONFIG_FILE
        cmd += " --command-config %s" % VerifiableProducer.CONFIG_FILE
                .help("Set reset policy (must be either 'earliest', 'latest', or 'none')");
                .metavar("CONFIG-FILE")
                .help("(DEPRECATED) Consumer config properties file" +
                        "This option will be removed in a future version. Use --command-config instead");

        parser.addArgument("--command-config")
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19625: consistency of command-line arguments for verifiable producer/consumer (#20390).

**Test**
Test modified in verifiable_client.py covering the corrected behaviour.
