# Bug 0787: SyncRequest.equals always returns true for ranges field (cassandra-3.0)

**Commit**: 9e2fe3d0ba · **Ticket**: n/a
**Subsystem**: Repair / messaging
**Category**: Logic error in condition

---

## Root Cause
The `equals()` method in `SyncRequest` compared `ranges` to itself (`ranges.equals(ranges)`) instead of comparing it to the other object's field (`ranges.equals(req.ranges)`). This tautology always evaluates to `true`, making the ranges field irrelevant to equality. Two `SyncRequest` objects with identical session IDs, initiator, src, and dst but different token ranges would be considered equal, which could allow duplicate or incorrectly deduplicated repair sync messages to go undetected.

### Before / After
```java
// BEFORE
return session.equals(req.session) &&
       initiator.equals(req.initiator) &&
       src.equals(req.src) &&
       dst.equals(req.dst) &&
       ranges.equals(ranges);   // compares this.ranges to itself — always true

// AFTER
return session.equals(req.session) &&
       initiator.equals(req.initiator) &&
       src.equals(req.src) &&
       dst.equals(req.dst) &&
       ranges.equals(req.ranges);  // correctly compares against the other object
```

---

## How to Spot
In any hand-written `equals()` method, each field comparison must reference `other.field` (or the local variable name for the cast argument) on the right-hand side. A field appearing on both sides of `.equals()` is always a bug. Code review or static analysis (e.g. SpotBugs `EQ_COMPARING_PARAMETER_TO_ITSELF`) catches this trivially.

---

## Test
No test added.
