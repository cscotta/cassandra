# Bug 0187: UnifiedCompactionTask Wrong Parent Constructor Passes ignoreOverlaps as gcBefore

**Commit**: 9c6e671434 · **Ticket**: CASSANDRA-18757
**Subsystem**: db/compaction/unified
**Category**: Wrong constant / default value

---

## Root Cause

`UnifiedCompactionTask`'s constructor called the four-argument parent constructor `CompactionTask(cfs, txn, gcBefore, ignoreOverlapsInExpirationCheck)`, which interprets the fourth argument as the `ignoreOverlaps` boolean flag read from `strategy.getController().getIgnoreOverlapsInExpirationCheck()`. However, `UnifiedCompactionTask` sets its own `ignoreOverlaps` behavior via `this.controller` after construction, so the correct parent constructor is the three-argument form `CompactionTask(cfs, txn, gcBefore)`. Using the four-argument constructor caused UCS compactions to silently inherit an incorrect expiration-check overlap policy (derived from `TimeWindowCompactionStrategy` options rather than the UCS controller) instead of reading it from the controller at runtime.

### Before / After

```java
// BEFORE
super(cfs, txn, gcBefore, strategy.getController().getIgnoreOverlapsInExpirationCheck());
```

```java
// AFTER
super(cfs, txn, gcBefore);
```

---

## How to Spot

When a subclass constructor delegates to a parent constructor by forwarding fields, verify that the selected constructor overload matches the semantic intent: in particular, watch for overloads that look identical except for an extra boolean flag that the subclass already manages independently. A review should check that `keepOriginals` / `ignoreOverlaps` are not set to a static value that the subclass then overrides from a different source.

---

## Test

`testIgnoreOverlapsUCSTrue` and `testIgnoreOverlapsUCSFalse` verify that a `UnifiedCompactionStrategy`-produced task has `keepOriginals == false` regardless of the `UNSAFE_AGGRESSIVE_SSTABLE_EXPIRATION` option value.
