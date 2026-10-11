# Bug 1516: Build Break — Remove Dead onSuspected Method from CqlConfigHelper

**Commit**: f759881505 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/hadoop/cql3
**Category**: Missing override of default method

---

## Root Cause

`CqlConfigHelper` contained an anonymous `Host.StateListener` implementation that overrode `onSuspected(Host host)`. When the Java driver removed `onSuspected` from the `StateListener` interface, the override became a dead method that no longer satisfied any interface contract. Its presence caused a compile-time or runtime mismatch and broke the build. Removing the dead override restores compatibility with the updated driver interface.

### Before / After

```java
// BEFORE
@Override
public void onSuspected(Host host)
{
}

@Override
public HostDistance distance(Host host)
{
```

```java
// AFTER
@Override
public HostDistance distance(Host host)
{
```

---

## How to Spot

When updating a third-party library dependency, scan `@Override` annotations in adapter/anonymous classes that implement the library's interfaces. Any `@Override` targeting a method that was removed in the new version will either fail to compile or silently become a dead method, both of which signal a broken interface contract.

---

## Test

No test added.
