**Commit**: 06a744ac3c · **Ticket**: KAFKA-19980
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
1. Enabled ConfigCommand#validatePropsKey to support the `$` symbol. 2. Added UT and IT for the change.

**Before/After**
```java
// Before
      if (!propsKey.toString.matches("[a-zA-Z0-9._-]*")) {
        verifyConfig(client, brokerId, config);
    private void verifyConfig(Admin client, Optional<String> brokerId, Map<String, String> config) throws Exception {
        TestUtils.waitForCondition(() -> {
            Map<String, String> current = getConfigEntryStream(client, configResource)
                    .filter(configEntry -> Objects.nonNull(configEntry.value()))
                    .collect(Collectors.toMap(ConfigEntry::name, ConfigEntry::value));
            return config.entrySet().stream().allMatch(e -> e.getValue().equals(current.get(e.getKey())));
        }, 10000, config + " are not updated");
        TestUtils.waitForCondition(() -> {
            Map<String, String> current = getConfigEntryStream(client, configResource)
                .filter(configEntry -> Objects.nonNull(configEntry.value()))

// After
      // Allows the '$' symbol to support valid logger names for internal classes (e.g. org.apache.kafka.server.quota.ClientQuotaManager$ThrottledChannelReaper)
      if (!propsKey.toString.matches("[$a-zA-Z0-9._-]*")) {
    @ClusterTest
    public void testBrokerLoggerConfigUpdate() throws Exception {
        List<String> alterOpts = Stream.concat(entityOp(Optional.of(defaultBrokerId)).stream(),
            Stream.of("--entity-type", "broker-loggers", "--alter")).toList();

        verifyBrokerLoggerConfigUpdate(alterOpts);

        // Test for the --broker-logger alias
        verifyBrokerLoggerConfigUpdate(List.of("--broker-logger", defaultBrokerId, "--alter"));
    }
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19980 configcommand#validatepropskey should accept `$` symbol (#21125).

**Test**
Test modified in ConfigCommandIntegrationTest.java covering the corrected behaviour.
