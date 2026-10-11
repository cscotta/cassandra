**Commit**: 899cdb598a · **Ticket**: KAFKA-18811
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
This PR includes a new flag in VerifiableShareConsumer.java called
command.config to include a properties file for admin client
related configs

Co-authored-by: Andrew Schofield <aschofield@confluent.io>

Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
    private final String brokerHostandPort;
                                   String brokerHostandPort,
        this.brokerHostandPort = brokerHostandPort;
                Properties adminClientProps = new Properties();
                adminClientProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerHostandPort);

                Admin adminClient = Admin.create(adminClientProps);

        parser.addArgument("--consumer.config")
            .help("Consumer config properties file (config options shared with command line parameters will be overridden).");
        String configFile = res.getString("consumer.config");
        String brokerHostandPort = res.getString("bootstrapServer");

// After
    private final Admin adminClient;
                                   Admin adminClient,
        this.adminClient = adminClient;
        parser.addArgument("--command-config")
            .dest("commandConfig")
            .help("Config properties file (config options shared with command line parameters will be overridden).");
        String configFile = res.getString("commandConfig");
        String brokerHostAndPort = res.getString("bootstrapServer");
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerHostAndPort);
        Properties adminClientProps = new Properties();
        if (configFile != null) {
            try {
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18811: added command configs to admin client as well in verifiableshareconsumer (#19130).

**Test**
No test added.
