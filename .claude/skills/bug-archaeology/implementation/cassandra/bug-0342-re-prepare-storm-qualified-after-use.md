# Bug 0342: Infinite Re-Prepare Storm on Qualified Statements After USE

**Commit**: 13632e9a99 · **Ticket**: CASSANDRA-15252
**Subsystem**: src/java/org/apache/cassandra/cql3/QueryProcessor.java
**Category**: State not cleaned up

---

## Root Cause

When a client issued `USE ks1` and then prepared a fully-qualified statement like `SELECT * FROM ks2.table`, the server computed the prepared statement ID by concatenating the query string with the current USE keyspace (`ks1`). The driver, however, computed the ID using no keyspace (since the statement is fully qualified). These two IDs diverge, so when the driver re-executes the statement it sends an ID the server doesn't recognize, triggering a UNPREPARED response. The driver re-prepares, the server again stores it under the USE-qualified ID, the driver again sends the unqualified ID, and the loop repeats indefinitely. The fix changes `prepare()` to store fully-qualified statements under *both* the old ID (with USE keyspace) and the new correct ID (without keyspace), and to return the new ID once all nodes in the cluster have been upgraded to a version that supports it. The minimum cluster version is checked via gossip (`Gossiper.instance.getMinVersion()`).

### Before / After

```java
// BEFORE – always incorporates the USE keyspace into the prepared statement ID
return storePreparedStatement(queryString, clientState.getRawKeyspace(), prepared, forThrift);
```

```java
// AFTER – for fully-qualified statements, store under both IDs and return the new one
//         only when the whole cluster supports it
if (prepared.keyspace != null) {
    ResultMessage.Prepared newBehavior = storePreparedStatement(queryString, null, prepared, forThrift);
    ResultMessage.Prepared oldBehavior = clientState.getRawKeyspace() != null
        ? storePreparedStatement(queryString, clientState.getRawKeyspace(), prepared, forThrift) : newBehavior;
    CassandraVersion minVersion = Gossiper.instance.getMinVersion(20, TimeUnit.MILLISECONDS);
    return (minVersion meets threshold) ? newBehavior : oldBehavior;
}
```

---

## How to Spot

Whenever a cache key is derived from both an explicit parameter and an implicit mutable context (such as the current keyspace from a `USE` statement), check whether client and server can independently compute different keys for the same logical entity. If the client omits context that the server includes, a mismatch loop becomes possible. Review all prepared-statement ID computation paths for sources of context that the client cannot replicate.

---

## Test

`ReprepareTestBase` and `ReprepareNewBehaviourTest` add distributed in-JVM tests that verify no re-prepare storm occurs when qualified statements are used after `USE`, across both old-behavior and new-behavior code paths.
