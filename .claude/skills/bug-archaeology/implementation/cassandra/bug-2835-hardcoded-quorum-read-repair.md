# Bug 2835: Read repair uses hardcoded QUORUM instead of request consistency level

**Commit**: 93539ffd2c · **Ticket**: CASSANDRA-1316

## Detection Rule
When constructing a repair response handler during a digest mismatch, pass the original request's `consistency_level` rather than a hardcoded level such as `ConsistencyLevel.QUORUM`.

## Code Smell
`rs.getQuorumResponseHandler(new ReadResponseResolver(...), ConsistencyLevel.QUORUM)` ignores the caller's `consistency_level` variable. If the original request used `ONE`, `ALL`, or any other level, the repair re-read is done at `QUORUM`, which either over-blocks or under-replicates.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: hardcoded-consistency-level
- severity: silent-wrong-result
- subsystem: paxos-lwt

## Root Cause
A botched merge introduced a hardcoded `ConsistencyLevel.QUORUM` in the digest-mismatch repair path instead of forwarding the client-supplied `consistency_level`. Reads at non-QUORUM consistency levels would perform the repair read at the wrong consistency, potentially returning stale data or blocking unnecessarily.

## Example
```java
// BEFORE
QuorumResponseHandler<Row> handler = rs.getQuorumResponseHandler(
    new ReadResponseResolver(command.table), ConsistencyLevel.QUORUM);

// AFTER
QuorumResponseHandler<Row> handler = rs.getQuorumResponseHandler(
    new ReadResponseResolver(command.table), consistency_level);
```
