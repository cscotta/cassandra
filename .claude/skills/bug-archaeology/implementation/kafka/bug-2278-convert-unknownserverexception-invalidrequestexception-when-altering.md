**Commit**: 894c4a9691 · **Ticket**: KAFKA-17525
**Subsystem**: server/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            int pushIntervalMs = Integer.parseInt(properties.getProperty(PUSH_INTERVAL_MS));
            List<String> patterns = Arrays.asList(properties.getProperty(CLIENT_MATCH_PATTERN).split(","));

// After
import org.apache.kafka.common.errors.InvalidConfigurationException;
import java.util.stream.Stream;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;
    @ClusterTest(types = {Type.CO_KRAFT, Type.KRAFT})
    public void testIntervalMsParser(ClusterInstance clusterInstance) {
        List<String> alterOpts = asList("--bootstrap-server", clusterInstance.bootstrapServers(),
                "--alter", "--entity-type", "client-metrics", "--entity-name", "test", "--add-config", "interval.ms=bbb");
        try (Admin client = clusterInstance.createAdminClient()) {
            ConfigCommand.ConfigCommandOptions addOpts = new ConfigCommand.ConfigCommandOptions(toArray(alterOpts));

            Throwable e = assertThrows(ExecutionException.class, () -> ConfigCommand.alterConfig(client, addOpts));
```

**How to spot**
When reviewing server/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17525 convert the unknownserverexception to invalidrequestexception when altering client-metrics config at runtime.

**Test**
Test modified in ClientTelemetryTest.java covering the corrected behaviour.
