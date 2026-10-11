# Bug 1493: Fix SSTableLoader Unable to Connect Encrypted Nodes

**Commit**: 563cea14b4 · **Ticket**: CASSANDRA-7585
**Subsystem**: src/java/org/apache/cassandra/streaming, src/java/org/apache/cassandra/tools
**Category**: Missing override of default method

---

## Root Cause

`SSTableLoader` (and `BulkLoader`) used the streaming subsystem's `ConnectionHandler.connect()` helper directly, which always created a plain `OutboundTcpConnectionPool.newSocket()` connection. There was no way for the bulk loader to inject a TLS-aware socket factory. The streaming framework had no `StreamConnectionFactory` abstraction, so the only connection path was hardcoded to unencrypted TCP. The fix introduces a `StreamConnectionFactory` interface with a `createConnection(InetAddress)` method, a `DefaultConnectionFactory` implementing the existing plain-TCP logic, and a hook on `StreamPlan.connectionFactory(...)`. `SSTableLoader.Client` gains an overridable `getConnectionFactory()` method, and `BulkLoader` overrides it to supply a factory that reads the cluster's server encryption options (via a custom config URL) and wraps the socket with SSLContext when required.

### Before / After

```java
// BEFORE — SSTableLoader always used plain TCP, no hook to inject TLS
StreamPlan plan = new StreamPlan("Bulk Load");
// ... ConnectionHandler.connect() called OutboundTcpConnectionPool.newSocket() directly
```

```java
// AFTER — factory is pluggable; BulkLoader overrides it for TLS
StreamPlan plan = new StreamPlan("Bulk Load").connectionFactory(client.getConnectionFactory());

// SSTableLoader.Client base implementation returns plain factory
public StreamConnectionFactory getConnectionFactory() {
    return new DefaultConnectionFactory();
}
// BulkLoader.ExternalClient overrides to return TLS-aware factory
```

---

## How to Spot

Any subsystem that opens network connections with a hardcoded socket creation helper will be unable to support encryption without a refactor. Connection creation should be separated behind a factory interface from the start so callers (especially tools like sstableloader) can plug in their own socket configuration.

---

## Test

No test added.
