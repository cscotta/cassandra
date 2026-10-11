# Bug 1584: Fix Assertion Error in CL.ANY Timeout Handling

**Commit**: afa304e338 · **Ticket**: CASSANDRA-7364
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: Logic error in condition

---

## Root Cause

When a write with `ConsistencyLevel.ANY` timed out (no replica acknowledged the write), the timeout handler iterated over all natural and pending endpoints and submitted hints for each one unconditionally. This caused an assertion error because the local node was included — attempting to hint yourself is invalid — and `shouldHint()` was never consulted to check whether the target actually needs a hint.

### Before / After

```java
// BEFORE
for (InetAddress target : Iterables.concat(naturalEndpoints, pendingEndpoints))
    submitHint((RowMutation) mutation, target, null);
```

```java
// AFTER
for (InetAddress target : Iterables.concat(naturalEndpoints, pendingEndpoints))
{
    // local writes can timeout, but cannot be dropped, so there is no need to hint or retry
    if (!target.equals(FBUtilities.getBroadcastAddress()) && shouldHint(target))
        submitHint((RowMutation) mutation, target, null);
}
```

---

## How to Spot

Whenever a loop submits hints to a list of endpoints without first filtering out the local node and checking `shouldHint()`, it may attempt to self-hint and bypass the hint eligibility check. Review all CL timeout handlers that call `submitHint` to ensure the local address guard and `shouldHint` gate are present.

---

## Test

No test added.
