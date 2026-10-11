# Bug 0790: SyncRequest.equals always returns true for ranges field (second cassandra-3.X backport)

**Commit**: 55e8f51851 · **Ticket**: n/a
**Subsystem**: Repair / messaging
**Category**: Logic error in condition

---

## Root Cause
This is a second backport of the same fix (see bug-0787, bug-0789) to another `cassandra-3.X` integration point. The `equals()` method in `SyncRequest` contained `ranges.equals(ranges)` — a self-comparison that always returns `true` — instead of `ranges.equals(req.ranges)`. The identical one-character edit (`ranges` → `req.ranges`) was required on this branch as well because the bug existed in the shared ancestry before the branch diverged.

### Before / After
```java
// BEFORE
ranges.equals(ranges);   // self-comparison — always true

// AFTER
ranges.equals(req.ranges);  // compare against the other object's field
```

---

## How to Spot
See bug-0787. When the same logical bug is present on multiple branches, each branch needs its own commit. Tracking such backports explicitly in CHANGES.txt entries (which were absent here) helps confirm full coverage across the release line.

---

## Test
No test added.
