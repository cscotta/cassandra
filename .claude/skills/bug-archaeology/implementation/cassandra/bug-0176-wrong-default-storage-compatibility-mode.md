# Bug 0176: Wrong Default Storage Compatibility Mode Breaks Simulator Tests

**Commit**: 9db161f038 · **Ticket**: CASSANDRA-19058
**Subsystem**: config, net/MessagingService, simulator
**Category**: Wrong constant / default value

---

## Root Cause

`CassandraRelevantProperties.JUNIT_STORAGE_COMPATIBILITY_MODE` defaulted to `StorageCompatibilityMode.CASSANDRA_4`, which caused `MessagingService.Version.CURRENT` to resolve to `VERSION_40` in tests that do not load a full config. This forced the simulator to use an older messaging protocol version, breaking Paxos simulation tests that depend on the v5.1 protocol path. The `RunnableActionScheduler.Kind` enum also lacked an `IMMEDIATE` case in the `ClusterSimulation` switch statement, causing an `AssertionError` whenever that variant was chosen.

### Before / After

```java
// BEFORE
JUNIT_STORAGE_COMPATIBILITY_MODE("cassandra.junit_storage_compatibility_mode",
    StorageCompatibilityMode.CASSANDRA_4.toString()),
```

```java
// AFTER
JUNIT_STORAGE_COMPATIBILITY_MODE("cassandra.junit_storage_compatibility_mode",
    StorageCompatibilityMode.NONE.toString()),
```

---

## How to Spot

When a property default controls protocol/format negotiation, verify that its default matches the "current" version rather than a legacy compatibility value. A wrong default here silently downgrades all tests that do not override the property, masking new-protocol code paths without any compilation error or obvious failure message.

---

## Test

The fix resolves `ShortPaxosSimulationTest.simulationTest`, which fails non-deterministically when the messaging version is downgraded; no additional test was added.
