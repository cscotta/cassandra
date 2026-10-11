# Bug 1539: Fix Merge — Remove Relocation Code from TokenMetadata

**Commit**: da3fd5d7ac · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/locator, src/java/org/apache/cassandra/service
**Category**: Logic error in condition

---

## Root Cause

A bad merge left stale "relocation" token logic in `TokenMetadata.calculatePendingRanges()` and `PendingRangeCalculatorService`. The early-exit condition in `calculatePendingRanges` incorrectly included `relocatingTokens.isEmpty()` as a guard, meaning any non-empty relocation map would bypass the early return and execute duplicate or incorrect pending range calculations. The fix removes all relocation-related code paths (the feature was being removed), and delegates `calculatePendingRanges` entirely to `TokenMetadata` instead of duplicating the logic in `PendingRangeCalculatorService`.

### Before / After

```java
// BEFORE — early exit wrongly guarded by relocatingTokens
if (bootstrapTokens.isEmpty() && leavingEndpoints.isEmpty()
    && movingEndpoints.isEmpty() && relocatingTokens.isEmpty())
{ ... return; }
// ... then relocation block applied stale pending ranges
```

```java
// AFTER — relocation concept removed entirely
if (bootstrapTokens.isEmpty() && leavingEndpoints.isEmpty()
    && movingEndpoints.isEmpty())
{ ... return; }
```

---

## How to Spot

After removing a feature, search for all references to the removed concept (here `relocat*`) and ensure none remain in conditional guards or state cleanup paths. Leftover conditions can invert or short-circuit important early-return logic.

---

## Test

No test added. (Accompanying `RelocateTest.java` was also deleted.)
