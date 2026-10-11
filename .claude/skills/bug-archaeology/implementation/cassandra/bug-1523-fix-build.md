# Bug 1523: Fix Build — Missing onSuspected Override in CqlConfigHelper (Branch Variant)

**Commit**: 4b6d6ebd8c · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/hadoop/cql3
**Category**: Missing override of default method

---

## Root Cause

Same root cause as Bug 1520: after a merge the anonymous `Host.StateListener` implementation in `CqlConfigHelper` was missing the `onSuspected(Host)` method required by the interface, breaking compilation. This is the same fix applied to a parallel branch. The `@Override`-annotated empty stub is re-added to restore full interface compliance.

### Before / After

```java
// BEFORE — required interface method absent; build broken
@Override
public HostDistance distance(Host host)
{
```

```java
// AFTER — onSuspected stub re-added
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

When the same build fix appears on multiple branches, the underlying interface change was not propagated consistently across all active merge lines. Track interface changes in third-party driver upgrades and apply them to every affected branch simultaneously.

---

## Test

No test added. The fix is a compile-time correction.
