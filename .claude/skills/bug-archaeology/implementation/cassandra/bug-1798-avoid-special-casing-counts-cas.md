# Bug 1798: Avoid Special-Casing Received/Expected Counts in CAS Timeout

**Commit**: 1d4caf4e4e · **Ticket**: CASSANDRA-6595
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: Wrong constant / default value

---

## Root Cause

When a CAS (Paxos) operation exhausted its retry budget without reaching quorum, `StorageProxy` threw `WriteTimeoutException` and `ReadTimeoutException` with hard-coded sentinel values of `-1` for both `received` and `blockFor`. These sentinel values were meaningless and misleading to clients: the native protocol serialises them directly into the timeout error response, so drivers and operators saw `-1` acknowledged / `-1` required, making it impossible to understand what consistency level was being targeted or how many responses were actually expected. The fix passes `0` for received (none reached quorum in time) and computes the true `blockFor` value from the consistency level and keyspace.

### Before / After

```java
// BEFORE
throw new WriteTimeoutException(WriteType.CAS, consistencyForPaxos, -1, -1);
```

```java
// AFTER
throw new WriteTimeoutException(WriteType.CAS, consistencyForPaxos, 0,
        consistencyForPaxos.blockFor(Keyspace.open(keyspaceName)));
```

---

## How to Spot

Hard-coded sentinel integers (-1, 0, Integer.MAX_VALUE) in exception constructors that encode protocol-visible counts should raise a flag. Verify that every `received` / `blockFor` / `acknowledged` argument to timeout exceptions reflects a genuinely computed value, not a placeholder that gets serialised to the client.

---

## Test

No test added.
