# Bug 0115: Replica Filtering Protection Fetches Entire Partition When Static Row Is Unresolved but No Static Predicate Exists

**Commit**: bd99331bf2 · **Ticket**: CASSANDRA-20243
**Subsystem**: `src/java/org/apache/cassandra/service/reads/ReplicaFilteringProtection.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

In Replica Filtering Protection (RFP), whenever a static row needed to be "completed" (i.e., re-fetched from a full replica), the code unconditionally set `unresolvedStatic = true`, which caused the entire partition to be fetched regardless of whether the query actually had a predicate on a static column. When a query filtered on a non-static clustering column only, RFP would still fetch the full partition to satisfy the phantom static row resolution, allowing unmatched regular rows from that partition to slip through to the coordinator's result set because no post-filter was applied when there were no regular column predicates.

### Before / After

```java
// BEFORE – marks static as unresolved unconditionally
if (row.isStatic())
    unresolvedStatic = true;
```

```java
// AFTER – only marks static unresolved when a static column predicate actually exists
if (row.isStatic())
    unresolvedStatic = command.rowFilter().hasStaticExpression();
```

---

## How to Spot

RFP is a correctness mechanism that must not widen the scope of re-fetches beyond what the query actually needs. Whenever "completion" logic branches on row type (static vs. regular), reviewers should verify that each branch is guarded by the corresponding predicate type check. Any unconditional escalation from a partial-row fetch to a full-partition fetch in the presence of a filter that does not cover the static row is a candidate for false positives.

---

## Test

`ReplicaFilteringProtectionTest#testMissingStaticRowWithNonStaticExpression` and `StrictFilteringTest#testMissingStaticRowWithNonStaticExpression` set up a two-node cluster with divergent static and regular rows, then assert that a query with only a non-static predicate does not return extra rows through the full-partition fetch path.
