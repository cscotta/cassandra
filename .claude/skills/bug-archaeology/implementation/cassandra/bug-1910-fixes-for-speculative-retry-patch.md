# Bug 1910: Speculative retry sends reads to wrong or already-contacted replicas

**Commit**: 20c419b948 · **Ticket**: CASSANDRA-5932
**Subsystem**: src/java/org/apache/cassandra/service/AbstractReadExecutor.java, src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: Logic error in condition

---

## Root Cause

The original speculative-retry implementation mixed up which replicas had been contacted and which were available as speculative extras. `SpeculativeReadExecutor` computed the extra replica as `unfiltered.get(handler.endpoints.size())` — an index into the full unfiltered list using the number of contacted endpoints — which could select a replica already in the query set when read-repair decisions changed the ordering. Additionally, `SpeculateAlwaysExecutor` iterated `unfiltered` instead of the filtered `endpoints`, meaning it could try to contact replicas excluded by consistency-level filtering. The rewrite separates the "always-speculating" and "maybe-speculating" cases into distinct executor classes, pre-computes the extra replica at selection time, and tracks whether speculation actually fired so that read-repair retries are directed only at genuinely contacted replicas.

### Before / After

```java
// BEFORE — extra replica computed from position in unfiltered list; could be already-contacted
InetAddress endpoint = unfiltered.get(handler.endpoints.size());
MessagingService.instance().sendRR(scommand.createMessage(), endpoint, handler);

// BEFORE — read-repair retried to handler.endpoints (which excluded the speculative replica)
for (InetAddress endpoint : exec.handler.endpoints)
    MessagingService.instance().sendRR(message, endpoint, repairHandler);
```

```java
// AFTER — extra replica pre-selected and appended before execution
InetAddress extraReplica = Iterables.getLast(targetReplicas);
MessagingService.instance().sendRR(retryCommand.createMessage(), extraReplica, handler);

// AFTER — read-repair uses getContactedReplicas() which reflects actual speculation
for (InetAddress endpoint : exec.getContactedReplicas())
    MessagingService.instance().sendRR(message, endpoint, repairHandler);
```

---

## How to Spot

When a "speculative" or "extra" request is constructed by indexing into one list using the size of a different list, verify that both lists share the same ordering and membership invariants. The safest pattern is to pre-compute all participants before any requests are sent and expose them through a single accessor.

---

## Test

No test added.
