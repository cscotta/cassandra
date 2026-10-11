**Commit**: 9dc9973c1c · **Ticket**: KAFKA-18863
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
                                       Supplier<List<ErrorReporter<SourceRecord>>> errorReportersSupplier) {
                time, statusBackingStore);
        byte[] key = retryWithToleranceOperator.execute(context, () -> keyConverterPlugin.get().fromConnectData(record.topic(), headers, record.keySchema(), record.key()),
                Stage.KEY_CONVERTER, keyConverterPlugin.get().getClass());
        byte[] value = retryWithToleranceOperator.execute(context, () -> valueConverterPlugin.get().fromConnectData(record.topic(), headers, record.valueSchema(), record.value()),
                Stage.VALUE_CONVERTER, valueConverterPlugin.get().getClass());
                byte[] rawHeader = headerConverterPlugin.get().fromConnectHeader(topic, key, header.schema(), header.value());
                result.add(key, rawHeader);
    public <R extends ConnectRecord<R>> List<TransformationStage<R>> transformationStages(ConnectorTaskId connectorTaskId, ConnectMetrics metrics) {

// After
import org.apache.kafka.connect.runtime.isolation.LoaderSwap;
import java.util.function.Function;
                                       Supplier<List<ErrorReporter<SourceRecord>>> errorReportersSupplier,
                                       Function<ClassLoader, LoaderSwap> pluginLoaderSwapper) {
                time, statusBackingStore, pluginLoaderSwapper);
        byte[] key = retryWithToleranceOperator.execute(context, () -> {
            try (LoaderSwap swap = pluginLoaderSwapper.apply(keyConverterPlugin.get().getClass().getClassLoader())) {
                return keyConverterPlugin.get().fromConnectData(record.topic(), headers, record.keySchema(), record.key());
            }
        }, Stage.KEY_CONVERTER, keyConverterPlugin.get().getClass());
        byte[] value = retryWithToleranceOperator.execute(context, () -> {
            try (LoaderSwap swap = pluginLoaderSwapper.apply(valueConverterPlugin.get().getClass().getClassLoader())) {
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18863: connect multiversion support (versioned connector creation and related changes) (#17743).

**Test**
Test modified in AbstractWorkerSourceTaskTest.java covering the corrected behaviour.
