# Bug 0398: StreamSession#onChannelClose Deadlock on Flush

**Commit**: dc5337bf95 · **Ticket**: CASSANDRA-15892
**Subsystem**: src/java/org/apache/cassandra/streaming/StreamSession.java
**Category**: Deadlock

---

## Root Cause

`StreamSession.onChannelClose()` was declared `synchronized`, meaning it acquired the intrinsic lock on the `StreamSession` instance. During a streaming flush, the Netty pipeline could call `onChannelClose` from a Netty I/O thread while the streaming thread already held the `StreamSession` lock and was waiting on a flush operation that itself needed to invoke a Netty operation on the same channel. This classic lock-ordering inversion (streaming thread holds `StreamSession` lock, waits on Netty; Netty thread waits on `StreamSession` lock) produced a deadlock. The fix removes the `synchronized` modifier entirely: `channels` is a `ConcurrentHashMap` and `messageSender.close()` is independently thread-safe, so no coarse-grained lock is needed.

### Before / After

```java
// BEFORE
private synchronized void onChannelClose(Channel channel)
{
    if (channels.remove(channel.id()) != null && channels.isEmpty())
        messageSender.close();
}
```

```java
// AFTER
private void onChannelClose(Channel channel)
{
    if (channels.remove(channel.id()) != null && channels.isEmpty())
        messageSender.close();
}
```

---

## How to Spot

`synchronized` methods that call into Netty pipeline operations (or any callback that may re-enter the same lock from a different thread) are deadlock-prone. During review, trace every code path reachable from within a `synchronized` block: if any path leads to blocking on an I/O thread that itself needs the same lock, the synchronization must be replaced with lock-free data structures or finer-grained locking.

---

## Test

No new test added; the fix is a single-keyword removal validated by the existing streaming integration test suite.
