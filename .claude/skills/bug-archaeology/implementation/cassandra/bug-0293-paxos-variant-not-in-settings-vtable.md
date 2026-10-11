# Bug 0293: paxos_variant Change via JMX Not Reflected in Settings Virtual Table

**Commit**: 1c70149ef5 · **Ticket**: CASSANDRA-17738
**Subsystem**: service / paxos / config
**Category**: State not cleaned up

---

## Root Cause

`Paxos.setPaxosVariant()` updated only the internal static field `PAXOS_VARIANT` but did not propagate the new value back to `DatabaseDescriptor` (and therefore `Config`). The Settings virtual table reads from `Config` via `DatabaseDescriptor.getPaxosVariant()`, so any runtime change made through `StorageProxy.setPaxosVariant()` (exposed over JMX) was invisible to the virtual table. The `Config` field and the in-memory `PAXOS_VARIANT` were silently out of sync.

A separate issue fixed in the same commit: the config parameter `enable_uuid_sstable_identifiers` was renamed to `uuid_sstable_identifiers_enabled` to conform to the project's naming convention; usages in `Config`, `DatabaseDescriptor`, `StartupChecks`, and YAML were all updated consistently.

### Before / After

```java
// BEFORE - setPaxosVariant only updates static field
public static void setPaxosVariant(Config.PaxosVariant paxosVariant)
{
    Preconditions.checkNotNull(paxosVariant);
    PAXOS_VARIANT = paxosVariant;
}
```

```java
// AFTER - also writes back to Config via DatabaseDescriptor
public static void setPaxosVariant(Config.PaxosVariant paxosVariant)
{
    Preconditions.checkNotNull(paxosVariant);
    PAXOS_VARIANT = paxosVariant;
    DatabaseDescriptor.setPaxosVariant(paxosVariant);
}
```

---

## How to Spot

Whenever a subsystem caches a configuration value in its own static field in addition to `Config`, both copies must be kept in sync on every write. During review, search for `static … PAXOS_VARIANT` (or similar shadow fields) and verify that every setter mutating the shadow also calls the corresponding `DatabaseDescriptor.setXxx()`. A simpler design is to eliminate the shadow field entirely and always read from `DatabaseDescriptor`.

---

## Test

`StorageProxyTest.testSetGetPaxosVariant()` asserts that after `StorageProxy.instance.setPaxosVariant("v2")`, both `StorageProxy.instance.getPaxosVariant()` and `DatabaseDescriptor.getPaxosVariant()` return `v2`.
