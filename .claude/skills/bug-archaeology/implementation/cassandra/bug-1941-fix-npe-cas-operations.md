# Bug 1941: NPE and TTL-expiry race in CAS operations due to shared UpdateParameters

**Commit**: 6ad62c3f4a · **Ticket**: CASSANDRA-5925
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/ModificationStatement.java, src/java/org/apache/cassandra/db/SystemKeyspace.java
**Category**: Missing null / bounds check

---

## Root Cause

Two related problems existed in CAS (lightweight transaction) execution. First, the same `UpdateParameters` instance was used both to build the mutation and to build the condition columns. When a very short TTL was specified, the condition columns could expire before the comparison was performed, causing the CAS to incorrectly compare against tombstones rather than live data. The fix creates separate `UpdateParameters` for the condition — with TTL forced to zero — so condition columns never expire prematurely. Second, `savePaxosProposal` in `SystemKeyspace` omitted the `in_progress_ballot` column from the UPDATE statement, so on a server restart the in-progress ballot was never persisted, leading to a NullPointerException when Paxos recovery tried to read it.

### Before / After

```java
// BEFORE — single params instance with caller TTL used for both
UpdateParameters params = new UpdateParameters(cfm, variables,
    getTimestamp(queryState.getTimestamp(), variables), getTimeToLive(variables), null);
ColumnFamily updates  = updateForKey(key, clusteringPrefix, params);
ColumnFamily expected = buildConditions(key, clusteringPrefix, params);
```

```java
// AFTER — separate params; condition TTL is always 0
UpdateParameters updParams = new UpdateParameters(cfm, variables,
    queryState.getTimestamp(), getTimeToLive(variables), null);
ColumnFamily updates = updateForKey(key, clusteringPrefix, updParams);

UpdateParameters condParams = new UpdateParameters(cfm, variables,
    queryState.getTimestamp(), 0, null);
ColumnFamily expected = buildConditions(key, clusteringPrefix, condParams);
```

```java
// BEFORE — in_progress_ballot missing from Paxos persistence
"UPDATE %s USING TIMESTAMP %d AND TTL %d SET proposal = 0x%s WHERE ..."

// AFTER — in_progress_ballot included
"UPDATE %s USING TIMESTAMP %d AND TTL %d SET in_progress_ballot = %s, proposal = 0x%s WHERE ..."
```

---

## How to Spot

In CAS / conditional-update paths, verify that the `UpdateParameters` used for building the expected (condition) side does not carry a TTL from the write request. Also audit Paxos state-persistence queries to confirm every field required for recovery is persisted.

---

## Test

No test added.
