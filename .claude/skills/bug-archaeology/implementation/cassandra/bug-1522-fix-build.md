# Bug 1522: Fix Build — Spurious @Override on onSuspected in CqlConfigHelper

**Commit**: 9faf905743 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/hadoop/cql3
**Category**: Missing override of default method

---

## Root Cause

An upstream merge introduced an `@Override` annotation on the `onSuspected(Host)` method inside the anonymous `Host.StateListener` implementation in `CqlConfigHelper`, but on this branch the `StateListener` interface did not declare that method. Using `@Override` on a method that does not exist in the supertype is a compile error in Java. The fix removes the erroneous `@Override` annotation.

### Before / After

```java
// BEFORE — @Override present but method not in interface on this branch
@Override
public void onSuspected(Host host)
{
}
```

```java
// AFTER — annotation removed; method is now a plain extra method (or will be removed as dead code)
public void onSuspected(Host host)
{
}
```

---

## How to Spot

When merging branches that track different versions of a third-party driver interface, `@Override` annotations can become invalid. Build failures catch this immediately, but reviewers should verify that merged `@Override` stubs actually correspond to methods declared in the interface version used on the target branch.

---

## Test

No test added. The fix is a compile-time correction.
