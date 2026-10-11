**Commit**: 7b865f2b22 · **Ticket**: KAFKA-17347
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
When KIP-714 was developed, the entity type of client-metrics was added to the kafka-configs.sh tool. The idea was to have two forms of specifying the name and type of a client metrics config resource, either --entity-type client-metrics --entity-name NAME or --client-metrics NAME. This style of alias is used for all of the entity types. Unfortunately, the --client-metrics form was not implemented.

**Before/After**
```java
// Before
    val allOpt: OptionSpecBuilder = parser.accepts("all", "List all configs for the given topic, broker, or broker-logger entity (includes static configuration when the entity type is brokers)")
    private val entityDefault: OptionSpecBuilder = parser.accepts("entity-default", "Default entity name for clients/users/brokers/ips (applies to corresponding entity type in command line)")
      if (options.has(describeOpt) && entityTypeVals.contains(BrokerLoggerConfigType) && !hasEntityName)
        throw new IllegalArgumentException(s"an entity name must be specified with --describe of ${entityTypeVals.mkString(",")}")

// After


    val allOpt: OptionSpecBuilder = parser.accepts("all", "List all configs for the given entity, including static configs if available.")
    private val entityDefault: OptionSpecBuilder = parser.accepts("entity-default", "Default entity name for clients/users/brokers/ips (applies to corresponding entity type)")
    val clientMetrics: OptionSpec[String] = parser.accepts("client-metrics", "The client metrics config resource name.")
      .withRequiredArg
      .ofType(classOf[String])
      (clientMetrics, ConfigType.CLIENT_METRICS),
      if (options.has(describeOpt)) {
        if (!(entityTypeVals.contains(ConfigType.USER) ||
          entityTypeVals.contains(ConfigType.CLIENT) ||
          entityTypeVals.contains(ConfigType.BROKER) ||
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17347 add missing client-metrics option to kafka-configs.sh (#17046).

**Test**
Test modified in ConfigCommandIntegrationTest.java covering the corrected behaviour.
