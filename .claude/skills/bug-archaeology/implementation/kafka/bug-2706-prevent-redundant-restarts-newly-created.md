**Commit**: 53ec055394 · **Ticket**: KAFKA-17105
**Subsystem**: connect/runtime
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
                // rebalance after connector removal to ensure that existing tasks are balanced among workers
                if (configState.contains(connector))
                connectorConfigUpdates.add(connector);
                if (!configState.contains(connector))
                connectorConfigUpdates.add(connector);

// After
                if (configState.contains(connector)) {
                    // rebalance after connector removal to ensure that existing tasks are balanced among workers
                } else {
                    connectorConfigUpdates.add(connector);
                }

                if (!configState.contains(connector)) {
                } else {
                    // Only need to restart the connector if it already existed
                    connectorConfigUpdates.add(connector);
                }

```

**How to spot**
When reviewing connect/runtime code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17105: prevent redundant restarts for newly-created connectors (#16581).

**Test**
Test modified in DistributedHerderTest.java covering the corrected behaviour.
