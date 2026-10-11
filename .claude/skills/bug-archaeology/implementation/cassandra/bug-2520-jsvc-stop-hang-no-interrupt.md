# Bug 2520: Cassandra hangs on jsvc stop because TCustomServerSocket never closes on interrupt

**Commit**: 7d2fbf9854 · **Ticket**: CASSANDRA-3302

## Detection Rule
When a class extends `TServerTransport` (Thrift), check that `interrupt()` is overridden to unblock a thread blocked in `accept()`; without it, the jsvc shutdown signal cannot wake the server socket thread and the process hangs indefinitely.

## Code Smell
`TCustomServerSocket extends TServerTransport` with no `interrupt()` override — Thrift's `AbstractNonblockingServer` calls `interrupt()` on the transport to stop accepting, but the default implementation is a no-op, leaving the `accept()` call blocked forever after `stop()` is requested.

## Tags
- scope: cassandra
- category: missing-override
- code-pattern: missing-interrupt-override
- severity: hang
- subsystem: network-messaging

## Root Cause
The jsvc daemon stop signal causes Cassandra to call `TServer.stop()`, which in turn calls `serverTransport_.interrupt()` on the `TServerTransport`. `TCustomServerSocket` did not override `interrupt()`, so the server thread remained blocked in `serverSocket_.accept()`. The fix adds an `interrupt()` override that calls `close()`, which closes the underlying `ServerSocket` and causes `accept()` to throw a `SocketException`, unblocking the thread.

## Example
```java
// BEFORE — no interrupt() override, hang on stop
public class TCustomServerSocket extends TServerTransport {
    @Override
    public void close() {
        if (serverSocket_ != null && !serverSocket_.isClosed())
            try { serverSocket_.close(); } catch (IOException e) { ... }
        serverSocket_ = null;
    }
    // interrupt() not overridden — no-op from base class
}
```
```java
// AFTER
@Override
public void interrupt() {
    close();
}
```
