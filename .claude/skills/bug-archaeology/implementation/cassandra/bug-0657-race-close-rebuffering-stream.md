# Bug 0657: Race Condition When Closing Stream Sessions

**Commit**: 83822d12d8 · **Ticket**: CASSANDRA-13852
**Subsystem**: src/java/org/apache/cassandra/net/async/RebufferingByteBufDataInputPlus.java
**Category**: Race condition / missing synchronization

---

## Root Cause

In `RebufferingByteBufDataInputPlus.available()`, the original code checked the `closed` flag before computing the number of buffered bytes. If the stream was closed between the `closed` check and the moment the reader tried to consume the last bytes, the consumer would never see the `EOFException`, leading to a hang or incorrect behavior when the session was being closed. The fix reorders the check: compute the available byte count first, and only throw `EOFException` when *both* the count is zero *and* the stream is closed, so that any remaining in-flight bytes are still returned to the caller.

### Before / After

```java
// BEFORE
public int available() throws EOFException
{
    if (closed)
        throw new EOFException();

    final int availableBytes = queuedByteCount.get()
        + (buffer != null ? buffer.remaining() : 0);
    ...
}
```

```java
// AFTER
public int available() throws EOFException
{
    final int availableBytes = queuedByteCount.get()
        + (buffer != null ? buffer.remaining() : 0);

    if (availableBytes == 0 && closed)
        throw new EOFException();
    ...
}
```

---

## How to Spot

Any method that checks a "terminal" flag (closed, done, cancelled) before reading a data value is susceptible to this pattern: if the flag is set concurrently with the last write of data, legitimate data can be discarded or mis-signaled as an error. Reviewers should look for flag checks that precede—rather than follow—the actual availability computation and ask whether the ordering matters under concurrent state transitions.

---

## Test

The added test exercises concurrent close and read on `RebufferingByteBufDataInputPlus` to verify that buffered bytes are visible to `available()` even when the stream has been flagged as closed.
