# Bug 1871: fix tracing adding 2x activity instead of threadName

**Commit**: 854de4ed56 · **Ticket**: n/a
**Subsystem**: tracing (TraceState)
**Category**: Wrong constant / default value

---

## Root Cause

The `TraceState.trace()` method logged the `message` (activity string) twice — once correctly
under the `"activity"` column and once more under the `"thread"` column — because the column
name passed for the thread field was `"activity"` instead of `"thread"`, and the value used was
the message string rather than `Thread.currentThread().getName()`.  This meant trace events
contained the activity text in both columns while the thread name was never recorded.
Additionally, `Thread.currentThread().getName()` was called inside the asynchronously-submitted
`WrappedRunnable`, which would return the name of the tracing stage thread rather than the
originating thread.  The fix captures `threadName` before submitting the task.

### Before / After

```java
// BEFORE – wrong column name ("activity") and wrong value (message) for the thread column;
//           thread name captured on wrong thread
Tracing.addColumn(cf, Tracing.buildName(cfMeta, eventId, ByteBufferUtil.bytes("activity")), message);
```

```java
// AFTER – thread name captured before async dispatch, stored under "thread" column
final String threadName = Thread.currentThread().getName();
// ... inside WrappedRunnable ...
Tracing.addColumn(cf, Tracing.buildName(cfMeta, eventId, ByteBufferUtil.bytes("thread")), threadName);
```

---

## How to Spot

When a runnable is submitted to an executor and references `Thread.currentThread()` inside its
body, verify whether the intent is to capture the submitting thread or the executing thread.
Also check that column/field name constants match the intended semantic — a copy-paste of a
nearby `addColumn` call is a common source of this class of error.

---

## Test

No test added.
