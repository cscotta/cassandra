# Bug 0225: Down Nodes Counter Always Zero in nodetool describecluster

**Commit**: 5655a33bc0 · **Ticket**: CASSANDRA-18512
**Subsystem**: tools/nodetool
**Category**: Logic error in condition

---

## Root Cause

`DescribeCluster` compared endpoint addresses formatted with `toString()` against the set of unreachable node strings, but `unreachableNodes` is built using `getHostAddressAndPort()` format. The two representations differ (e.g., `toString()` may include extra detail), so the `contains` check always returns false and the down-node counter is never incremented.

### Before / After

```java
// BEFORE
if (unreachableNodes.contains(endpoint.toString()))
    downNodes++;
```

```java
// AFTER
if (unreachableNodes.contains(endpoint.getHostAddressAndPort()))
    downNodes++;
```

---

## How to Spot

Whenever a collection is built by one code path and consumed by another using a `.contains()` check, verify that both sides produce the same string representation. A mismatch between `Object.toString()` and a domain-specific accessor like `getHostAddressAndPort()` is a common source of false negatives in membership tests.

---

## Test

No test added.
