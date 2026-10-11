# Bug 1012: Streaming task completion skipped in non-MV case

**Commit**: 814bc7065e · **Ticket**: CASSANDRA-10803
**Subsystem**: Streaming
**Category**: Logic error in condition

---

## Root Cause
`StreamReceiveTask.OnCompletionRunnable.run()` calls `task.session.taskCompleted(task)` to signal to the stream session that a receive task has finished.  The call was placed inside the inner `if (hasViews)` block rather than after it.  For tables with no materialized views (`hasViews == false`) the method reached the end of the `if` block without calling `taskCompleted`, so the streaming session never progressed past that task.  The fix moves `task.session.taskCompleted(task)` to the outer scope, one closing brace further out, so it executes regardless of whether the table has views.

### Before / After
```java
// BEFORE – taskCompleted called only when hasViews is true
if (hasViews)
{
    if (cfs != null)
    {
        ...cache invalidation...
        task.session.taskCompleted(task);  // ← only reached inside hasViews
    }
}

// AFTER – taskCompleted always called after the if block
if (hasViews)
{
    if (cfs != null)
    {
        ...cache invalidation...
    }
}
task.session.taskCompleted(task);  // ← always reached
```

---

## How to Spot
Signal / callback calls that are placed inside a conditional block rather than after it are prone to being silently skipped.  When reviewing streaming or state-machine completion paths, check that progress signals are unconditionally reached for all non-error paths.  Misplaced closing braces are a classic source of this error and are hard to spot in deeply indented code without examining brace alignment carefully.

---

## Test
No new test added; the fix is covered by the change in brace scope and existing streaming integration tests.
