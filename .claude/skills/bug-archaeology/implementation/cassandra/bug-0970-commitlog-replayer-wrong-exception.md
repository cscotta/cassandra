# Bug 0970: CommitLogReplayer declares wrong checked exception type

**Commit**: 30d3b29ab2 · **Ticket**: n/a
**Subsystem**: Commit log
**Category**: Wrong constant / default value

---

## Root Cause
`CommitLogReplayer` contained an anonymous `WrappedRunnable` whose `runMayThrow()` override declared `throws IOException`. The correct contract for `WrappedRunnable.runMayThrow()` requires `throws ExecutionException`. Using the wrong exception type caused a compilation failure because the override's checked-exception declaration was incompatible with the base class signature.

### Before / After
```java
// BEFORE
public void runMayThrow() throws IOException

// AFTER
public void runMayThrow() throws ExecutionException
```

---

## How to Spot
When overriding a method whose checked exception is tightly constrained by a supertype, verify that the declared `throws` clause matches the parent exactly. A mismatch will produce a build error, but in code review the risk is that a developer may mask the compile error by broadening the exception type rather than correcting it — which would compile but could swallow real failures or change error-handling semantics.

---

## Test
No test added.
