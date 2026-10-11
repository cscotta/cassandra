**Commit**: ad3369859c · **Ticket**: KAFKA-18419
**Subsystem**: connect/runtime
**Category**: Incorrect filtering / wrong query result

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
            ConfigInfos connectorConfigInfo = validateConnectorPluginSpecifiedConfigs(connectorProps, validatedConnectorConfig, enrichedConfigDef, connector, reportStage);
    private final Map<String, Exception> invalidConnectors;
    private final Map<String, Map<String, Exception>> invalidVersions;
    private void validate(String connectorName, VersionRange range) throws Exception {
            throw new Exception(invalidConnectors.get(connectorName));
            throw new Exception(invalidVersions.get(connectorName).get(version));
                WorkerConfig.KEY_CONVERTER_CLASS_CONFIG, WorkerConfig.KEY_CONVERTER_VERSION, connProps, workerConfig, Converter.class);
                WorkerConfig.VALUE_CONVERTER_CLASS_CONFIG, WorkerConfig.VALUE_CONVERTER_VERSION, connProps, workerConfig, Converter.class);

// After
            // the order of operations here is important, converter validations can add error messages to the connector config
            // which are collected and converted to ConfigInfos in validateConnectorPluginSpecifiedConfigs
            ConfigInfos connectorConfigInfo = validateConnectorPluginSpecifiedConfigs(connectorProps, validatedConnectorConfig, enrichedConfigDef, connector, reportStage);
import org.apache.kafka.connect.errors.ConnectException;
    private final Map<String, Throwable> invalidConnectors;
    private final Map<String, Map<String, VersionedPluginLoadingException>> invalidVersions;
    private void validate(String connectorName, VersionRange range) throws ConnectException, VersionedPluginLoadingException {
            throw new ConnectException(invalidConnectors.get(connectorName));
            throw new VersionedPluginLoadingException(invalidVersions.get(connectorName).get(version).getMessage());
import org.apache.kafka.connect.runtime.isolation.PluginType;
                WorkerConfig.KEY_CONVERTER_CLASS_CONFIG, WorkerConfig.KEY_CONVERTER_VERSION, connProps, workerConfig, PluginType.CONVERTER);
                WorkerConfig.VALUE_CONVERTER_CLASS_CONFIG, WorkerConfig.VALUE_CONVER
```

**How to spot**
When reviewing connect/runtime code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18419: kip-891 connect multiversion support (transformation and predicate changes) (#17742).

**Test**
Test modified in AbstractHerderTest.java covering the corrected behaviour.
