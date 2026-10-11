# Bug 2951: DC quorum response handler compares byte arrays instead of datacenter membership

**Commit**: ccafdada13 · **Ticket**: CASSANDRA-492

## Detection Rule
When a response handler uses `waitList.contains(message.getFrom())` to decide whether a response counts toward a quorum, check whether the list was populated from the correct source and whether InetAddress equality works via the list's `contains` implementation.

## Code Smell
`DatacenterQuorumResponseHandler` accepted a `List<InetAddress> waitList` from the caller and decremented `blockFor` only when `waitList.contains(message.getFrom())`; this relied on the caller supplying the correct list and did not reflect actual datacenter proximity at response time.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: stale-waitlist-vs-snitch
- severity: silent-wrong-result
- subsystem: paxos-lwt

## Root Cause
The `DatacenterShardStategy` passed `getLocalEndPoints()` as the wait-list but also had a bug where `foundCount` was never incremented in the endpoint-of-interest loop (`foundCount++` was missing before `continue`), causing the wrong replicas to be selected. Additionally, the response handler needed to check datacenter co-location dynamically via the `IEndPointSnitch` rather than a static pre-built list, because the list could be stale or incorrect. The fix removes the wait-list parameter entirely and uses `endpointsnitch.isInSameDataCenter()` at response time.

## Example
```java
// BEFORE: waitList may be wrong; foundCount never incremented
if (waitList.contains(message.getFrom())) {
    blockFor--;
}
```
```java
// AFTER: snitch-based check at response time
if (endpointsnitch.isInSameDataCenter(localEndpoint, message.getFrom())) {
    blockFor--;
}
```
