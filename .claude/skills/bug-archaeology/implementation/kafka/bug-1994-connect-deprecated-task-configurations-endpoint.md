**Commit**: 4f1688742e · **Ticket**: KAFKA-15387
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
    protected Map<ConnectorTaskId, Map<String, String>> buildTasksConfig(String connector) {
        final ClusterConfigState configState = configBackingStore.snapshot();

        if (!configState.contains(connector))
            return Collections.emptyMap();

        Map<ConnectorTaskId, Map<String, String>> configs = new HashMap<>();
        for (ConnectorTaskId cti : configState.tasks(connector)) {
            configs.put(cti, configState.rawTaskConfig(cti));
        }

        return configs;

// After

```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15387: remove connect's deprecated task configurations endpoint (#17412).

**Test**
Test modified in ConnectWorkerIntegrationTest.java covering the corrected behaviour.
