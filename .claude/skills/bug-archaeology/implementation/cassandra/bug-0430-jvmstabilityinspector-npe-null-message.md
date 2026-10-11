# Bug 0430: NPE in JVMStabilityInspector When Exception Has No Message

**Commit**: 068d87acfb · **Ticket**: CASSANDRA-16294
**Subsystem**: src/java/org/apache/cassandra/utils/JVMStabilityInspector.java
**Category**: Missing null / bounds check

---

## Root Cause

`JVMStabilityInspector.inspectThrowable` checks whether a caught `FileNotFoundException` or `SocketException` signals file-handle exhaustion by calling `t.getMessage().contains("Too many open files")`. Both exception constructors accept a `null` message, and `SocketException()` (no-argument constructor) always produces a `null` message. Calling `.contains()` on a `null` string throws a secondary `NullPointerException` inside the stability inspector itself, masking the original exception and preventing normal error handling.

### Before / After

```java
// BEFORE
if (t instanceof FileNotFoundException || t instanceof SocketException)
    if (t.getMessage().contains("Too many open files"))
        isUnstable = true;
```

```java
// AFTER
if (t instanceof FileNotFoundException || t instanceof SocketException)
    if (t.getMessage() != null && t.getMessage().contains("Too many open files"))
        isUnstable = true;
```

---

## How to Spot

Any call to `Throwable.getMessage()` (or `getCause().getMessage()`) without a prior null check is a latent NPE. `Throwable.getMessage()` is documented to return `null` when no message was provided, and most no-argument exception constructors do exactly that. Reviewers should flag every bare `e.getMessage().someMethod()` pattern as requiring a null guard.

---

## Test

`JVMStabilityInspectorTest` was extended to invoke `inspectThrowable` with a no-argument `SocketException()` and a no-argument `FileNotFoundException()`, asserting that neither triggers the kill switch (i.e., neither throws nor marks the node as unstable).
