# Bug 0471: Replica-Side Filtering Returns Stale Data with CL > 1

**Commit**: dd255ffa07 · **Ticket**: CASSANDRA-8272
**Subsystem**: `src/java/org/apache/cassandra/service/DataResolver.java`, `src/java/org/apache/cassandra/service/ReplicaFilteringProtection.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When a query includes a row filter (secondary index or `ALLOW FILTERING`) and CL > ONE, each replica independently evaluates the filter against its local data before returning results. If replica A has an outdated row that still satisfies the filter, while replica B has the up-to-date version that does not satisfy the filter (but has not sent a tombstone to A), the coordinator's reconciliation phase would merge the results and include A's stale row — there is no tombstone to shadow it. The coordinator had no mechanism to detect or correct this: it performed a single-pass merge assuming replica responses were self-consistent. The fix introduces `ReplicaFilteringProtection`, a two-phase resolution strategy: a first pass identifies rows present in some replicas but absent in others (potentially outdated), skips them pessimistically from the count, and collects their primary keys for a second per-replica fetch. The second pass re-merges all results (original plus fetched) through normal read-repair, applying `rowFilter()` on the final merged output.

### Before / After

```java
// BEFORE (DataResolver.resolve)
int count = responses.size();
List<UnfilteredPartitionIterator> iters = new ArrayList<>(count);
InetAddress[] sources = new InetAddress[count];
for (int i = 0; i < count; i++) {
    MessageIn<ReadResponse> msg = responses.get(i);
    iters.add(msg.payload.makeIterator(command));
    sources[i] = msg.from;
}
// single pass merge — stale filtered rows survive
UnfilteredPartitionIterator merged = mergeWithShortReadProtection(iters, sources, mergedResultCounter);
```

```java
// AFTER (DataResolver.resolve)
if (!needsReplicaFilteringProtection()) {
    ResolveContext context = new ResolveContext(responses.size());
    return resolveWithReadRepair(context,
                                 i -> shortReadProtectedResponse(i, context),
                                 UnaryOperator.identity());
}
return resolveWithReplicaFilteringProtection();
// two-phase: first pass collects stale candidates, second pass
// re-queries replicas and merges with rowFilter applied
```

---

## How to Spot

Any read path that applies a server-side predicate (index lookup, ALLOW FILTERING) before returning results to the coordinator is susceptible to returning stale data at CL > ONE. A reviewer should ask: "Can a replica return a row matching the filter that a more up-to-date replica would not return — without providing a tombstone?" If yes, the coordinator needs a second-pass verification strategy. The signature is that `DataResolver` merges responses without any post-merge filter application.

---

## Test

No new test added in this commit (the fix is purely in production code; integration tests for this scenario are tracked separately under CASSANDRA-8273).
