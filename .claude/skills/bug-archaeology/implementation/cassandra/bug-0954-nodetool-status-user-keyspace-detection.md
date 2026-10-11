# Bug 0954: nodetool status ownership warning fires for single user keyspace

**Commit**: f4b21f5ffa · **Ticket**: CASSANDRA-10176
**Subsystem**: Tools / Schema
**Category**: Logic error in condition

---

## Root Cause
`StorageService.getOwnership()` used `getNonSystemKeyspaces()` to decide whether to show effective-ownership info. That method included `system_traces` and `system_auth` (replicated, but internal, keyspaces) in its result. The code then subtracted a hard-coded count for `system_traces`, but the condition compared with `> specialTableCount` instead of `> 1`, meaning that with a single real user keyspace plus `system_traces`, the inequality `nonSystemKeyspaces.size() > specialTableCount` evaluated to `2 > 1 = true` and threw an `IllegalStateException` ("Non-system keyspaces don't have the same replication settings") — even though there was only one user keyspace and the warning was completely unwarranted.

The fix introduces `getUserKeyspaces()` which subtracts both `system_traces` and `system_auth` from the non-system set, and rewrites the ownership logic to iterate over real user keyspaces and verify they actually share replication settings before complaining.

### Before / After
```java
// BEFORE
List<String> nonSystemKeyspaces = Schema.instance.getNonSystemKeyspaces();
int specialTableCount = 0;
if (nonSystemKeyspaces.contains("system_traces"))
    specialTableCount += 1;
if (nonSystemKeyspaces.size() > specialTableCount)
    throw new IllegalStateException("Non-system keyspaces don't have the same replication settings...");
keyspace = "system_traces";

// AFTER
List<String> userKeyspaces = Schema.instance.getUserKeyspaces();
if (userKeyspaces.size() > 0)
{
    keyspace = userKeyspaces.iterator().next();
    AbstractReplicationStrategy replicationStrategy =
        Schema.instance.getKeyspaceInstance(keyspace).getReplicationStrategy();
    for (String keyspaceName : userKeyspaces)
    {
        if (!Schema.instance.getKeyspaceInstance(keyspaceName)
                            .getReplicationStrategy().hasSameSettings(replicationStrategy))
            throw new IllegalStateException("Non-system keyspaces don't have the same replication settings...");
    }
}
else
{
    keyspace = "system_traces";
}
```

---

## How to Spot
Look for any code that calls `getNonSystemKeyspaces()` and then tries to carve out internal-but-replicated keyspaces (`system_traces`, `system_auth`) by name with a magic counter. Any such pattern is fragile — it will break as new replicated system keyspaces are added or as the set of replicated system keyspaces differs across versions. Prefer `getUserKeyspaces()` which maintains the exclusion list centrally in `Schema.replicatedSystemKeyspaceNames`.

---

## Test
No test added.
