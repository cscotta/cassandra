# Bug 0956: False resource-leak warning in CompressedChecksummedDataInput and OutboundTcpConnectionPool

**Commit**: ac7e2793de · **Ticket**: CASSANDRA-11084
**Subsystem**: Hints / Networking
**Category**: Resource leak

---

## Root Cause
Static analysis tools (e.g., FindBugs / ErrorProne) flagged two factory methods as potential resource leaks because the methods open or wrap a resource and hand ownership to the caller without closing it on all paths visible to the analyser. In `CompressedChecksummedDataInput.upgradeInput()`, the underlying channel from the supplied `ChecksummedDataInput` is transferred into a new `CompressedChecksummedDataInput`; the outer object's `close()` closes the channel, but the analyser cannot infer that. In `OutboundTcpConnectionPool.newSocket()`, a `SocketChannel` is created and wrapped in a `Socket`; the socket's `close()` closes the channel, but again the analyser cannot track the aliasing. Both methods were annotated with `@SuppressWarnings("resource")` and a brief comment explaining the transitive ownership, making the intent explicit without changing behaviour.

### Before / After
```java
// BEFORE
public static final CompressedChecksummedDataInput upgradeInput(
        ChecksummedDataInput input, ICompressor compressor) { ... }

// AFTER
// Closing the CompressedChecksummedDataInput will close the underlying channel.
@SuppressWarnings("resource")
public static final CompressedChecksummedDataInput upgradeInput(
        ChecksummedDataInput input, ICompressor compressor) { ... }

// BEFORE
public static Socket newSocket(InetAddress endpoint) throws IOException { ... }

// AFTER
// Closing the socket will close the underlying channel.
@SuppressWarnings("resource")
public static Socket newSocket(InetAddress endpoint) throws IOException { ... }
```

---

## How to Spot
When a factory method opens a low-level resource (channel, stream) and immediately wraps it in a higher-level closeable that owns it, static analysis tools often flag the inner resource as leaked because they cannot follow ownership-transfer semantics through constructor arguments. Before suppressing the warning, confirm that (a) the wrapper's `close()` genuinely closes the inner resource, (b) the factory has no early-exit code path that could leave the inner resource open, and (c) the suppression annotation is accompanied by a comment explaining the ownership contract. If any early-exit path exists, real cleanup code should be added.

---

## Test
No test added.
