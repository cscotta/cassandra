# Bug 2421: RangeSlice read repair targets un-queried nodes because resolver sources include all live endpoints

**Commit**: c3dc789415 · **Ticket**: CASSANDRA-3843

## Detection Rule

When a response resolver is initialized with an endpoint list before the actual read-callback endpoint list is finalized (e.g. after consistency-level trimming), it may include nodes that were never sent the query, causing spurious read-repair messages to un-queried nodes.

## Code Smell

`new RangeSliceResponseResolver(keyspace, liveEndpoints)` where `liveEndpoints` is the full candidate list rather than `handler.endpoints` (the trimmed list of nodes actually queried) — the resolver's source list and the actual message recipients are different sets.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: resolver-sources-not-matching-actual-recipients
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause

`StorageProxy` created a `RangeSliceResponseResolver` with `liveEndpoints` (all live replicas), then created a `ReadCallback` which may trim the list to a smaller `handler.endpoints` set based on the consistency level. The resolver's `sources` list still contained all live endpoints, so when it triggered read repair it would attempt to send repair messages to nodes that were never queried for this range slice — those nodes would receive unexpected repair data. The fix separates resolver construction from source assignment: the resolver is created with no sources, and `resolver.setSources(handler.endpoints)` is called after the handler (with its trimmed endpoint list) is ready.

## Example

```java
// BEFORE — sources set to all live endpoints before CL trimming
RangeSliceResponseResolver resolver =
    new RangeSliceResponseResolver(nodeCmd.keyspace, liveEndpoints);
ReadCallback<Iterable<Row>> handler = getReadCallback(resolver, nodeCmd, cl, liveEndpoints);
handler.assureSufficientLiveNodes();
// handler.endpoints may be smaller than liveEndpoints, but resolver still targets all
```

```java
// AFTER — sources set to the actually-queried endpoint set
RangeSliceResponseResolver resolver = new RangeSliceResponseResolver(nodeCmd.keyspace);
ReadCallback<Iterable<Row>> handler = getReadCallback(resolver, nodeCmd, cl, liveEndpoints);
handler.assureSufficientLiveNodes();
resolver.setSources(handler.endpoints);   // only nodes that were actually queried
```
