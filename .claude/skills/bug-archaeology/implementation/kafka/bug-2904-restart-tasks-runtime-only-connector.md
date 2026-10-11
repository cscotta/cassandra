**Commit**: eec8fd6a98 · **Ticket**: KAFKA-9228
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
        } else {

// After
        }
        if (!result) {
        if (!result) {
            Map<String, String> appliedConnectorConfig = configState.appliedConnectorConfig(connName);
            Map<String, String> currentConnectorConfig = configState.connectorConfig(connName);
            if (!Objects.equals(appliedConnectorConfig, currentConnectorConfig)) {
                log.debug("Forcing task restart for connector {} as its configuration appears to be updated", connName);
                result = true;
            }
        }
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-9228: restart tasks on runtime-only connector config changes (#16053).

**Test**
Test modified in ConnectWorkerIntegrationTest.java covering the corrected behaviour.
