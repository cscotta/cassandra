# Bug 0138: RFP Skipped When Row Filter Has Only Key Columns

**Commit**: 9da4bd412d · **Ticket**: CASSANDRA-19938
**Subsystem**: src/java/org/apache/cassandra/service/reads/DataResolver.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`DataResolver.usesReplicaFilteringProtection()` was designed to skip the Replica Filtering Protection (RFP) path when the row filter contained no non-key column expressions, because partition-key and clustering-key columns are immutable and never become stale. However, the method was also the gating condition that decided whether `preCountFilterForReplicaFilteringProtection()` was called, and that pre-count filter is needed even when only key-column expressions are present — it short-circuits the expensive re-query of potentially stale replicas. The fix splits the logic: `usesReplicaFilteringProtection` only returns `false` when the row filter is completely empty; `preCountFilterForReplicaFilteringProtection` returns an identity operator when there are no non-key expressions, preserving the correct no-op fast path while still enabling RFP when key-only filters are present.

### Before / After

```java
// BEFORE – skipped RFP entirely for key-column-only filters
private boolean usesReplicaFilteringProtection()
{
    // Key columns are immutable and should never need to participate in replica filtering
    if (!command.rowFilter().hasNonKeyExpressions())
        return false;
    ...
}
```

```java
// AFTER – only skip when filter is completely empty
private boolean usesReplicaFilteringProtection()
{
    if (command.rowFilter().isEmpty())
        return false;
    ...
}

// Key-only guard moved to the pre-count filter method
private UnaryOperator<PartitionIterator> preCountFilterForReplicaFilteringProtection()
{
    if (!command.rowFilter().hasNonKeyExpressions())
        return results -> results;
    ...
}
```

---

## How to Spot

When two related conditions (enablement of a subsystem vs. fast-path short-circuit inside that subsystem) are conflated in a single boolean guard, look for cases where the subsystem should activate but the inner fast path should still be skipped. Refactoring the guard into separate layers prevents one concern from silently disabling the other.

---

## Test

No new test added in this commit; the fix was validated by the existing RFP integration test suite.
