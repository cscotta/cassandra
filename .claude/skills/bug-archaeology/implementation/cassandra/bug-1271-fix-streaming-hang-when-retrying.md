# Bug 1271: Fix Streaming Hang When Retrying

**Commit**: a11bc78656 · **Ticket**: CASSANDRA-9132
**Subsystem**: src/java/org/apache/cassandra/streaming/messages/IncomingFileMessage.java
**Category**: Logic error in condition

---

## Root Cause

`IncomingFileMessage.deserialize()` wrapped the entire file-read in a single `catch (Throwable e)` block that called `session.doRetry(header, e)` for any exception. This meant that an `IOException` caused by the remote peer dying mid-transfer (EOF before the expected number of bytes were read) was treated as a retryable error. On retry the session attempted to re-read from the same dead or reset connection, which would again produce EOF, triggering another retry — an infinite loop that caused the streaming session to hang. The fix adds a specific `catch (IOException eof)` block that re-throws the exception immediately, so network/peer-failure IOExceptions propagate correctly and abort the session rather than retrying.

### Before / After

```java
// BEFORE
catch (Throwable e)
{
    session.doRetry(header, e);
    return null;
}
```

```java
// AFTER
catch (IOException eof)
{
    // Reached EOF before expected data — network/node failure, do not retry
    throw eof;
}
catch (Throwable e)
{
    // Otherwise, retry
    session.doRetry(header, e);
    return null;
}
```

---

## How to Spot

A broad `catch (Throwable)` or `catch (Exception)` that triggers a retry must distinguish between transient errors (worth retrying) and fatal/connection-level errors (where retrying the same connection will always fail). Any I/O deserialization loop that retries on `IOException` is a candidate for this class of hang: EOF and broken-pipe exceptions must be separated from recoverable decode errors.

---

## Test

No test added.
