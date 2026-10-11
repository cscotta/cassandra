# Bug 1733: Race-Prone Double Scrub of System Keyspace on Startup

**Commit**: ab2717b6f6 · **Ticket**: CASSANDRA-6797
**Subsystem**: src/java/org/apache/cassandra/service/CassandraDaemon.java
**Category**: Race condition / missing synchronization

---

## Root Cause

During `CassandraDaemon` startup, the system keyspace was scrubbed once early in the init sequence (before the general keyspace loop) and then scrubbed a second time when the loop over all keyspaces reached the system keyspace. Running `scrubDataDirectories` twice on the system keyspace introduced a race window: the first scrub could partially rename or remove files, and the second scrub would then encounter the partially-modified directory, risking corruption or incorrect cleanup of system metadata.

### Before / After

```java
// BEFORE: loop over all keyspaces with no exclusion
for (String keyspaceName : Schema.instance.getKeyspaces())
{
    for (CFMetaData cfm : Schema.instance.getKeyspaceMetaData(keyspaceName).values())
        ColumnFamilyStore.scrubDataDirectories(keyspaceName, cfm.cfName);
}
```

```java
// AFTER: skip system keyspace in the general loop
for (String keyspaceName : Schema.instance.getKeyspaces())
{
    // Skip system as we've already cleaned it
    if (keyspaceName.equals(Keyspace.SYSTEM_KS))
        continue;

    for (CFMetaData cfm : Schema.instance.getKeyspaceMetaData(keyspaceName).values())
        ColumnFamilyStore.scrubDataDirectories(keyspaceName, cfm.cfName);
}
```

---

## How to Spot

Whenever a startup sequence has a special-cased early action for one member of a collection, verify that the subsequent general loop explicitly skips that member. Duplicate file-system operations on the same directory during startup are particularly dangerous because they operate on partially initialised state.

---

## Test

No test added.
