# Bug 1694: Fix 6956 Patch (ColumnCounter Static Group Handling)

**Commit**: d8c29a3f7e · **Ticket**: n/a (follow-up to CASSANDRA-6956)
**Subsystem**: src/java/org/apache/cassandra/db/filter/ColumnCounter.java
**Category**: State not cleaned up

---

## Root Cause

The CASSANDRA-6956 patch introduced logic to avoid double-counting a static group when it is followed by a regular group: upon detecting the transition, the code set `previousGroupIsStatic = false` and returned early without updating `previous` to the current column's components. On the next column in the regular group, `previous` still held the components of the last static column, not the first regular-group column, causing the group-boundary comparison to use stale state and potentially miscount rows. Additionally, the `isSameGroup` check did not first verify that the static/non-static flag was consistent before comparing components, which could cause a static-to-regular transition to be treated as "same group". The fix updates `previous = current` before the early return and adds a `previousGroupIsStatic` equality check before the per-component loop.

### Before / After

```java
// BEFORE – previous not updated on static→regular transition
if (previousGroupIsStatic)
{
    previousGroupIsStatic = false;
    return;  // previous still points to last static column
}
```

```java
// AFTER – previous updated before early return; static flag checked first
boolean isSameGroup = previousGroupIsStatic == type.isStaticName(column.name());
if (isSameGroup) {
    for (int i = 0; i < toGroup; i++) { ... }
}
// ...
if (previousGroupIsStatic)
{
    previous = current;  // correct: advance pointer before discarding group
    previousGroupIsStatic = false;
    return;
}
```

---

## How to Spot

When an early-return path skips the normal "advance the cursor / update tracking state" step at the bottom of a loop body, the tracking variable is left pointing at stale data for the next iteration. Always verify that every exit path from an accumulation loop leaves the tracking state consistent.

---

## Test

No test added.
