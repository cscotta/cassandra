# Bug 0789: SyncRequest.equals always returns true for ranges field (cassandra-3.X backport)

**Commit**: 595f1da9ef · **Ticket**: n/a
**Subsystem**: Repair / messaging
**Category**: Logic error in condition

---

## Root Cause
This is a backport of the same fix applied in commit 9e2fe3d0ba (bug-0787) to the `cassandra-3.X` branch. The `equals()` method in `SyncRequest` compared `ranges` against itself (`ranges.equals(ranges)`) rather than against `req.ranges`, making the ranges field a no-op in equality checks. Two `SyncRequest` objects differing only in token ranges would be incorrectly considered equal.

### Before / After
```java
// BEFORE
ranges.equals(ranges);   // self-comparison — always true

// AFTER
ranges.equals(req.ranges);  // compare against the other object's field
```

---

## How to Spot
See bug-0787. The same self-comparison defect existed on multiple active maintenance branches and required separate backport commits.

---

## Test
No test added.
