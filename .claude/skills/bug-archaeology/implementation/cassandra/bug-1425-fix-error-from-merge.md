# Bug 1425: Fix Error from Merge (DatabaseDescriptor initialization order)

**Commit**: 95f9666975 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/config/DatabaseDescriptor.java
**Category**: Logic error in condition

---

## Root Cause

A merge reordering placed `throw new ExceptionInInitializerError(...)` before `JVMStabilityInspector.inspectThrowable(e)` in the static initializer's catch block. This meant the stability inspector—which is responsible for examining the throwable and potentially triggering shutdown hooks or additional diagnostics—was never reached before the exception propagated. The correct order is to inspect first, then rethrow.

### Before / After

```java
// BEFORE (wrong order after merge)
catch (Exception e)
{
    throw new ExceptionInInitializerError(e.getMessage() + "...");
    JVMStabilityInspector.inspectThrowable(e);  // unreachable
}
```

```java
// AFTER
catch (Exception e)
{
    JVMStabilityInspector.inspectThrowable(e);  // inspect before rethrowing
    throw new ExceptionInInitializerError(e.getMessage() + "...");
}
```

---

## How to Spot

In any catch block that contains both a side-effecting call (logging, metrics, shutdown coordination) and a `throw`, the throw must always be last. Merge conflicts in catch blocks are particularly prone to this inversion because both sides of a merge may insert lines at the same location. Code review after a merge should verify that all catch blocks still follow the inspect-then-rethrow pattern.

---

## Test

No test added.
