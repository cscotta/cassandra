# Bug 2029: Repair Hangs Silently When IncomingStreamReader Throws an Unexpected RuntimeException

**Commit**: 45e6d912cc · **Ticket**: CASSANDRA-5229
**Subsystem**: src/java/org/apache/cassandra/streaming/IncomingStreamReader.java
**Category**: Resource leak

---

## Root Cause

`IncomingStreamReader.read()` caught `IOException` and properly called `session.close(false)` to fail the streaming session on disk errors. However, it had no handler for `RuntimeException`. If an unexpected runtime error (e.g., an assertion, a deserialization failure, an OOM) occurred during stream reading, the exception propagated up without closing the session. The repair coordination layer then waited forever for a session completion notification that would never arrive, causing the repair to hang silently.

### Before / After

```java
// BEFORE — only IOException was handled
catch (IOException ex)
{
    retry();
    throw ex;
}
```

```java
// AFTER
catch (IOException ex)
{
    retry();
    throw ex;
}
catch (RuntimeException e)
{
    // if we encountered unexpected exception, fail this session
    session.close(false);
    throw e;
}
```

---

## How to Spot

In any session/connection/repair protocol where a coordinator waits for a completion callback, every exception path in the worker must call the failure notification before rethrowing. If only a subset of exception types (e.g., `IOException`) is handled, unchecked exceptions will silently strand the coordinator. Review try-catch blocks in streaming and repair workers for missing `RuntimeException` handlers.

---

## Test

No test added.
