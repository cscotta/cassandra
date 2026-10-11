**Commit**: 956d740c46 · **Ticket**: KAFKA-16068
**Subsystem**: connect/runtime
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
import org.apache.kafka.connect.errors.ConnectException;
        Map<String, String> config = defaultSinkConnectorProps();
        config.put(KEY_CONVERTER_CLASS_CONFIG, TestConverterWithPrivateConstructor.class.getName());
        Map<String, String> config = defaultSinkConnectorProps();
        config.put(KEY_CONVERTER_CLASS_CONFIG, TestConverterWithConstructorThatThrowsException.class.getName());
        Map<String, String> config = defaultSinkConnectorProps();
        config.put(HEADER_CONVERTER_CLASS_CONFIG, TestConverterWithPrivateConstructor.class.getName());
        Map<String, String> config = defaultSinkConnectorProps();
        config.put(HEADER_CONVERTER_CLASS_CONFIG, TestConverterWithConstructorThatThrowsException.class.getName());
    public static class TestConverterWithPrivateConstructor extends TestConverter {
        private TestConverterWithPrivateConstructor() {
        }

// After
import org.apache.kafka.connect.runtime.WorkerConfig;
import org.apache.kafka.connect.runtime.isolation.TestPlugins;
        TestPlugins.TestPlugin[] testPlugins = new TestPlugins.TestPlugin[] {
            TestPlugins.TestPlugin.BAD_PACKAGING_DEFAULT_CONSTRUCTOR_THROWS_CONVERTER,
            TestPlugins.TestPlugin.BAD_PACKAGING_DEFAULT_CONSTRUCTOR_THROWS_CONVERTER,
            TestPlugins.TestPlugin.BAD_PACKAGING_INNOCUOUS_CONNECTOR
        };
        workerProps.put(
                WorkerConfig.PLUGIN_PATH_CONFIG,
                TestPlugins.pluginPathJoined(testPlugins)
        );

```

**How to spot**
When reviewing connect/runtime code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16068: use testplugins mechanism in connectorvalidationintegrationtest to prevent error-level log spam in unrelate.

**Test**
Test modified in ConnectorValidationIntegrationTest.java covering the corrected behaviour.
