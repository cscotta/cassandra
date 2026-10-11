**Commit**: 4a37c2e18f · **Ticket**: KAFKA-16219
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
We observed some runs of the test suite caused CI pipelines to stall. A thread dump revealed that the test runner was blocked trying to read from a
socket, while attempting to close the socket [[0]]. It turns out this is
due to a bug in JDK which is very similar to JDK-8274524, but it affects
the else branch of `SSLSocketImpl::bruteForceCloseInput` [[1]] which wasn't
fixed in JDK-8274524. Since the blocking happens in a native call, the test runner's timeouts have
no effect as the blocked test runner thread doesn't seem to respond to
interrupts.

**Before/After**
```java
// Before
// (no removals)

// After
    // JDK has a bug where sockets may get blocked on a read operation if they are closed during a TLS handshake.
    // While rare, this would cause the CI pipeline to hang. We set a reasonably high SO_TIMEOUT to avoid blocking reads
    // indefinitely.
    // The JDK bug is similar to JDK-8274524, but it affects the else branch of SSLSocketImpl::bruteForceCloseInput
    // which wasn't fixed in it. Please refer to the comments in KAFKA-16219 for more information.
    private static final int SO_TIMEOUT_MS = 30_000;

                this.serverSocket.setSoTimeout(SO_TIMEOUT_MS);
                if (sslFactory != null) {
                    socket.setSoTimeout(SO_TIMEOUT_MS);
                }
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16219 set so_timeout in echoserver (#16354).

**Test**
Test modified in EchoServer.java covering the corrected behaviour.
