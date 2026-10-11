# Bug 1095: Fix Handling of Streaming EOF

**Commit**: 2267d79188 · **Ticket**: CASSANDRA-10206
**Subsystem**: streaming/messages/StreamMessage.java
**Category**: Logic error in condition

---

## Root Cause

`StreamMessage.deserialize` treated both `read() == 0` (channel buffer temporarily empty) and `read() < 0` (end-of-stream / socket closed) the same way: it returned `null` in either case. The comment even said "when socket gets closed… just return null." Callers that received `null` interpreted it as a transient no-data condition and continued polling, so a genuine EOF was silently swallowed instead of surfacing as a connection-closed error. The fix distinguishes the two cases: zero bytes read returns `null` as before (non-blocking, try again), while a negative return value throws `SocketException("End-of-stream reached")` so the streaming subsystem can react to the closed channel.

### Before / After

```java
// BEFORE
if (in.read(buff) > 0)
{
    buff.flip();
    return Type.get(buff.get()).inSerializer.deserialize(in, version, session);
}
else
{
    // when socket gets closed, there is a chance that buff is empty
    // in that case, just return null
    return null;
}
```

```java
// AFTER
int readBytes = in.read(buff);
if (readBytes > 0)
{
    buff.flip();
    return Type.get(buff.get()).inSerializer.deserialize(in, version, session);
}
else if (readBytes == 0)
{
    // input socket buffer was not filled yet
    return null;
}
else
{
    // possibly socket gets closed
    throw new SocketException("End-of-stream reached");
}
```

---

## How to Spot

Any NIO `read()` call that handles only `> 0` vs. `<= 0` conflates empty-buffer and EOF. The contract of `ReadableByteChannel.read()` specifies three distinct outcomes: positive (data), zero (no data yet), and negative (end-of-stream). Code that maps the latter two to the same result will silently ignore closed connections. Always check for `== -1` (or `< 0`) as a separate branch and propagate it as an error.

---

## Test

No test added.
