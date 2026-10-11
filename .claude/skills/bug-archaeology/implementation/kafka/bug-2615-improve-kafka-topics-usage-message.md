**Commit**: 3ddd8d0a0e · **Ticket**: KAFKA-17262
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
There is a lot of historical cruft in the usage message for kafka-topics.sh. For example, the --bootstrap-server option is required nowadays, but the usage message is written in some cases as if it was still optional. This PR just tightens up the usage message, removing out-of-date text and improving the grammar. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        private static final String KAFKA_CONFIGS_CLI_SUPPORTS_ALTERING_TOPIC_CONFIGS_WITH_A_BOOTSTRAP_SERVER =
                " (the kafka-configs CLI supports altering topic configs with a --bootstrap-server option)";
            commandConfigOpt = parser.accepts("command-config", "Property file containing configs to be passed to Admin Client. " +
                            "This is used only with --bootstrap-server option for describing and altering broker configs.")
            String kafkaConfigsCanAlterTopicConfigsViaBootstrapServer =
                    " (the kafka-configs CLI supports altering topic configs with a --bootstrap-server option)";
            deleteOpt = parser.accepts("delete", "Delete a topic");
            alterOpt = parser.accepts("alter", "Alter the number of partitions and replica assignment. " +
                    "Update the configuration of an existing topic via --alter is no longer supported here" +
                    kafkaConfigsCanAlterTopicConfigsViaBootstrapServer + ".");
            topicIdOpt = parser.accepts("topic-id", "The topic-id to describe." +
                            "This is used only with --bootstrap-server option for describing topics.")

// After
        private static final String KAFKA_CONFIGS_CLI_SUPPORTS_ALTERING_TOPIC_CONFIGS =
                " (To alter topic configurations, the kafka-configs tool can be used.)";
            commandConfigOpt = parser.accepts("command-config", "Property file containing configs to be passed to Admin Client.")
            deleteOpt = parser.accepts("delete", "Delete a topic.");
            alterOpt = parser.accepts("alter", "Alter the number of partitions and replica assignment." +
                    KAFKA_CONFIGS_CLI_SUPPORTS_ALTERING_TOPIC_CONFIGS);
            topicIdOpt = parser.accepts("topic-id", "The topic-id to describe.")
                            " It is supported only in combination with --create." +
                            KAFKA_CONFIGS_CLI_SUPPORTS_ALTERING_TOPIC_CONFIGS)
            deleteConfigOpt = parser.accepts("delete-config", "This option is no longer supported.")
                    "altered. If not supplied with --create, the topic uses the cluster default. (WARNING: If partitions are increased for a topic that has a key, the partition logic or orde
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17262 improve kafka-topics usage message (#16795).

**Test**
No test added.
