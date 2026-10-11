# Bug 0260: Inverted Datacenter Filter Drops All Responses in Single-DC Paxos Commit

**Commit**: acda6fad68 · **Ticket**: CASSANDRA-17999
**Subsystem**: src/java/org/apache/cassandra/service/paxos/PaxosCommit.java
**Category**: Logic error in condition

---

## Root Cause

In `PaxosCommit.response()`, when the consistency level is datacenter-local (e.g. `LOCAL_SERIAL`), the code is supposed to ignore responses from nodes that are *outside* the local datacenter. The condition was written without a logical negation: `if (consistencyForCommit.isDatacenterLocal() && InOurDc.endpoints().test(from)) return;` — which discards responses from nodes that *are* in the local datacenter and counts only those outside it. In a single-datacenter cluster there are no out-of-DC nodes, so no responses ever reached the quorum counter and every `LOCAL_SERIAL` write timed out with a `WriteTimeoutException`.

### Before / After

```java
// BEFORE — inverted: discards responses FROM our DC
if (consistencyForCommit.isDatacenterLocal() && InOurDc.endpoints().test(from))
    return;
```

```java
// AFTER — correct: discards responses from OTHER DCs
if (consistencyForCommit.isDatacenterLocal() && !InOurDc.endpoints().test(from))
    return;
```

---

## How to Spot

Any guard of the form `if (localDCCondition && isInOurDC(node)) ignore;` should be read as "ignore nodes in our DC", which is backwards for a local-consistency filter. When reviewing responses that count toward a datacenter-local quorum, the correct invariant is to ignore responses that are *not* in the local DC. A missing `!` before a membership test is easy to overlook because the surrounding logic reads naturally in prose.

---

## Test

`CASTest.testWriteTimeoutExceptionUsingPaxosInLwtPerformaceTest` creates a single-DC three-node cluster with `SimpleStrategy` RF=3 and exercises `IF NOT EXISTS` and `IF EXISTS` LWT writes at `LOCAL_QUORUM`, verifying they complete without a `WriteTimeoutException`.
