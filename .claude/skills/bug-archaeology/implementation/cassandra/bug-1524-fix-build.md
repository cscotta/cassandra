# Bug 1524: Fix Build — Missing onSuspected Override in CqlConfigHelper (Branch Variant)

**Commit**: 9f1e9575cb · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/hadoop/cql3
**Category**: Missing override of default method

---

## Root Cause

Same root cause as Bugs 1520 and 1523: a merge left the anonymous `Host.StateListener` implementation in `CqlConfigHelper` missing the `onSuspected(Host)` method, breaking compilation. This commit applies the identical fix — an `@Override`-annotated empty stub — to yet another branch in the cassandra-2.1 merge chain.

### Before / After

```java
// BEFORE — interface method missing; build fails
@Override
public HostDistance distance(Host host)
{
```

```java
// AFTER — onSuspected stub re-added with @Override
@Override
public void onSuspected(Host host)
{
}

@Override
public HostDistance distance(Host host)
{
```

---

## How to Spot

Repeated application of the same fix across many branch-merge commits is a sign of an under-synchronized multi-branch workflow. A shared utility or driver upgrade task should be coordinated so the fix lands once and is propagated cleanly rather than patched post-hoc on each branch.

---

## Test

No test added. The fix is a compile-time correction.
