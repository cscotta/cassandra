# Bug 0559: SimpleStrategy Does Not Validate Options at Construction Time

**Commit**: 47d4971b56 · **Ticket**: CASSANDRA-15007
**Subsystem**: `src/java/org/apache/cassandra/locator/SimpleStrategy.java`
**Category**: Missing null / bounds check

---

## Root Cause

`SimpleStrategy` called `ReplicationFactor.fromString(configOptions.get("replication_factor"))` in its constructor without first checking whether `replication_factor` was present in `configOptions`. `validateOptions()` performed the null check, but it was only called later as part of the schema-change validation flow. A caller who instantiated `SimpleStrategy` directly (bypassing the normal validation path, e.g., during deserialization or internal tooling) would receive a `NullPointerException` from `fromString(null)` rather than a meaningful `ConfigurationException`. The fix extracts the null check into a private `validateOptionsInternal` method called both from the constructor and from `validateOptions`.

### Before / After

```java
// BEFORE — constructor does no validation
public SimpleStrategy(...) {
    super(...);
    this.rf = ReplicationFactor.fromString(this.configOptions.get("replication_factor")); // NPE if missing
}

public void validateOptions() throws ConfigurationException {
    String rf = configOptions.get("replication_factor");
    if (rf == null)
        throw new ConfigurationException("SimpleStrategy requires a replication_factor strategy option.");
    validateReplicationFactor(rf);
}
```

```java
// AFTER
public SimpleStrategy(...) {
    super(...);
    validateOptionsInternal(configOptions);  // throws ConfigurationException early
    this.rf = ReplicationFactor.fromString(this.configOptions.get(REPLICATION_FACTOR));
}

private static void validateOptionsInternal(Map<String, String> configOptions) {
    if (configOptions.get(REPLICATION_FACTOR) == null)
        throw new ConfigurationException("SimpleStrategy requires a replication_factor strategy option.");
}
```

---

## How to Spot

When a constructor reads from a `Map` that was passed in by the caller without validating required keys, a `NullPointerException` can escape instead of a domain-specific exception. Review constructors of strategy/plugin classes that accept option maps: if they access keys before calling `validateOptions`, ensure the access is guarded or that validation is performed inline.

---

## Test

A new `testSimpleStrategyThrowsConfigurationException` unit test in `SimpleStrategyTest` constructs a `SimpleStrategy` with an empty config map and asserts that a `ConfigurationException` with the correct message is thrown.
