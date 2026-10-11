# Bug 2093: PropertyFileSnitch Rejects Valid Config When Default DC/Rack Is Set

**Commit**: 7093813870 · **Ticket**: CASSANDRA-5285
**Subsystem**: src/java/org/apache/cassandra/locator/PropertyFileSnitch.java
**Category**: Logic error in condition

---

## Root Cause

`PropertyFileSnitch` supports a `default` entry in `cassandra-topology.properties` that applies to any node whose address is not explicitly listed. During config reload the code always checked whether the local broadcast address appeared in the loaded map, and threw a `ConfigurationException` if it did not — even when a default DC/Rack was configured. Any deployment that relied on the default entry was broken every time the snitch reloaded its properties file.

### Before / After

```java
// BEFORE — throws even when a valid default entry is present
if (!reloadedMap.containsKey(FBUtilities.getBroadcastAddress()))
    throw new ConfigurationException(String.format(
        "Snitch definitions at %s do not define a location for this node's broadcast address %s",
        SNITCH_PROPERTIES_FILENAME, FBUtilities.getBroadcastAddress()));
```

```java
// AFTER — only throws when neither a default nor an explicit entry exists
if (defaultDCRack == null && !reloadedMap.containsKey(FBUtilities.getBroadcastAddress()))
    throw new ConfigurationException(String.format(
        "Snitch definitions at %s do not define a location for this node's broadcast address %s, nor does it provides a default",
        SNITCH_PROPERTIES_FILENAME, FBUtilities.getBroadcastAddress()));
```

---

## How to Spot

Validation that throws on a missing key should always account for every legitimate fallback path. When a feature supports an explicit-or-default pattern, the guard must check both before raising an error.

---

## Test

No test added.
