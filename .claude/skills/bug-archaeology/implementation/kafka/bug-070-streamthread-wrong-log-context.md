**Commit**: f9ccf83a7f · **Ticket**: KAFKA-18066
**Subsystem**: streams/src/main/java/org/apache/kafka/streams/processor/internals/StreamThread.java
**Category**: Logic error in condition

**Root cause**
`StreamThread.create()` built a `LogContext` prefixed with the new thread's ID, then obtained a `Logger` from it via `logContext.logger(StreamThread.class)`. That logger embedded the new thread's name. However, at the point of execution, the current thread is still the caller (not the new stream thread). Log messages like "Creating restore consumer client" and "Creating consumer client" therefore appeared under the new thread's name even though they ran in the caller's context, causing confusing log traces. The fix uses `LoggerFactory.getLogger(StreamThread.class)` for messages emitted by the caller, and passes the `LogContext` (rather than a pre-bound `Logger`) to sub-components so they create their own loggers.

**Before/After**
```java
// Before: logger bound to new thread name used in caller's execution context
final Logger log = logContext.logger(StreamThread.class);
log.info("Creating restore consumer client");   // misleading: runs in caller thread

// After: plain logger without thread context for caller-thread messages
final Logger log = LoggerFactory.getLogger(StreamThread.class);
log.info("Creating restore consumer client for thread {}", threadId);
```

**How to spot**
Any time a `LogContext` is built for a thread-to-be-created and immediately used to log messages in the current thread, the log entries will bear the wrong thread name. Loggers derived from a `LogContext` are only correct when they run inside that thread. Pass the `LogContext` to the thread itself; use a generic factory logger for setup code that runs in the parent thread.

**Test**
No new test was added. The change is a log-correctness improvement; tests in `ActiveTaskCreatorTest` and `StreamThreadTest` updated constructor call signatures to reflect the `LogContext` parameter change.
