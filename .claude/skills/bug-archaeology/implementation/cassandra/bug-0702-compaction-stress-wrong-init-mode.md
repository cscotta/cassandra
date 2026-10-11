# Bug 0702: CompactionStress Tool Crashes Due to Wrong Initialization Mode

**Commit**: 2369faab79 · **Ticket**: CASSANDRA-13188
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/CompactionStress.java
**Category**: Wrong constant / default value

---

## Root Cause

`CompactionStress` initialised `DatabaseDescriptor` via `toolInitialization()` rather than `daemonInitialization()`. Tool initialization leaves certain subsystems (in particular the compaction infrastructure and the storage engine's table-management code) in a reduced state that is insufficient for a tool that needs to actually perform compactions and manage SSTables on disk. Consequently, when the tool tried to write SSTables and run compaction passes, it hit assertions or missing components that are only set up by `daemonInitialization()`. Additionally, `StressCQLSSTableWriter` asserted that the table schema did not already exist rather than returning the existing store, so a second call (e.g., when the test re-runs over the same data directory) would crash.

### Before / After

```java
// BEFORE
static {
    DatabaseDescriptor.toolInitialization();
}
```

```java
// AFTER
static {
    DatabaseDescriptor.daemonInitialization();
}
```

```java
// BEFORE — StressCQLSSTableWriter crashes if table already registered
assert cfMetaData == null;
```

```java
// AFTER — returns existing store rather than asserting
if (cfMetaData != null)
    return Schema.instance.getColumnFamilyStoreInstance(cfMetaData.cfId);
```

---

## How to Spot

Any offline tool that writes real SSTables or drives compaction must use `daemonInitialization()` rather than the lighter `toolInitialization()`. The diagnostic symptom is an NPE or assertion error deep inside the storage engine during the first write, because storage-related singletons were never populated. Additionally, any `assert x == null` guard on schema registration in tool code will crash on repeated invocations; idempotent re-use should return the existing instance.

---

## Test

A new `CompactionStressTest` verifies that the tool can run a write pass followed by a compact pass against a real data directory without crashing.
