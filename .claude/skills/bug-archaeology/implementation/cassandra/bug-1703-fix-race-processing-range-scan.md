# Bug 1703: Fix race processing range scan responses

**Commit**: 4347982974 · **Ticket**: CASSANDRA-6820
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java, src/java/org/apache/cassandra/service/ReadCallback.java
**Category**: Race condition / missing synchronization

---

## Root Cause

When executing a range scan, `StorageProxy` passed the full `filteredEndpoints` list (all replicas contacted) to `ReadCallback`, but `ReadCallback` was designed to trigger read repair when it received responses from more replicas than `blockFor` required. Range scans did not support read repair at that time (CASSANDRA-6897), so receiving extra responses could cause the callback to attempt repair logic incorrectly or process responses out of order. The fix trims the endpoint list passed to `ReadCallback` to exactly `min(endpoints.size(), blockFor)` replicas, and adds an assertion inside `ReadCallback` to enforce that range-scan resolvers never receive a `blockfor` smaller than the endpoint count.

### Before / After

```java
// BEFORE — full replica list passed; extra responses could trigger unsupported repair path
ReadCallback<RangeSliceReply, Iterable<Row>> handler =
    new ReadCallback(resolver, consistency_level, nodeCmd, filteredEndpoints);
```

```java
// AFTER — endpoint list capped to blockFor
List<InetAddress> minimalEndpoints =
    filteredEndpoints.subList(0, Math.min(filteredEndpoints.size(), consistency_level.blockFor(keyspace)));
ReadCallback<RangeSliceReply, Iterable<Row>> handler =
    new ReadCallback<>(resolver, consistency_level, nodeCmd, minimalEndpoints);
```

---

## How to Spot

When a callback or handler is parameterized with both a consistency level and a replica list, verify that the list length matches what the callback expects. Passing more replicas than `blockFor` to a callback that triggers read repair on "bonus" responses will fire repair logic for code paths that have not been audited for it.

---

## Test

No test added.
