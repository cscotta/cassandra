# Bug 1520: Fix Build — Missing onSuspected Override in CqlConfigHelper

**Commit**: ef27851376 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/hadoop/cql3
**Category**: Missing override of default method

---

## Root Cause

The anonymous `Host.StateListener` implementation inside `CqlConfigHelper` was missing the `onSuspected(Host)` method required by the interface. A prior merge had removed the method body, causing a compilation failure. The fix re-adds the `@Override`-annotated empty stub so the anonymous class fully implements the interface contract.

### Before / After

```java
// BEFORE — method missing; anonymous class fails to compile
@Override
public HostDistance distance(Host host)
{
```

```java
// AFTER — required interface method re-added as empty stub
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

When an interface adds a new non-default abstract method, all anonymous/inner-class implementations must be updated. A build break is the strongest signal, but code reviewers can catch it by checking that every method in the target interface has a corresponding implementation in the anonymous class.

---

## Test

No test added. The fix is a compile-time correction; the build itself serves as the regression gate.
