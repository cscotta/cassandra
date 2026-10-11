# Bug 0441: NPE in StorageService When Thrift Server Not Yet Initialized

**Commit**: 521a6e2aa9 · **Ticket**: CASSANDRA-16127
**Subsystem**: src/java/org/apache/cassandra/service/CassandraDaemon.java, StorageService.java
**Category**: Missing null / bounds check

---

## Root Cause

`CassandraDaemon.thriftServer` and `nativeServer` were public fields accessed directly from `StorageService`. The `enablethrift` (and `enablebinary`) nodetool commands called `daemon.thriftServer.start()` without a null check, but `initializeNativeTransport()` (which sets both fields) is called during `setup()`, not at class-load time. If the JMX method was invoked before `setup()` completed, or in test configurations that skipped transport initialization, both fields were null, causing an NPE. Additionally, callers in `stop()` and `destroyNativeTransport()` accessed the same fields without guarding for null.

### Before / After

```java
// BEFORE — direct public field access in StorageService
daemon.thriftServer.start();   // NPE if setup() not yet run

// BEFORE — CassandraDaemon.stop()
thriftServer.stop();           // NPE if never initialized
nativeServer.stop();
```

```java
// AFTER — null-safe methods added to CassandraDaemon
public void startThriftServer() {
    if (thriftServer == null)
        throw new IllegalStateException("setup() must be called first for CassandraDaemon");
    thriftServer.start();
}
public void stopThriftServer() {
    if (thriftServer != null)
        thriftServer.stopAndAwaitTermination();
}
// StorageService calls daemon.startThriftServer() / daemon.stopThriftServer()
```

---

## How to Spot

Whenever a class exposes mutable state (especially server handles) as public non-final fields, any code path that references those fields must be audited for initialization order. Look for calls of the form `obj.field.method()` where `field` is set during an initialization method (`setup()`, `init()`) rather than in the constructor — these are candidates for NPEs whenever the JMX or management plane can be invoked before or concurrently with initialization.

---

## Test

`ClientNetworkStopStartTest` starts a cluster in various configurations and repeatedly calls `enablethrift`/`disablethrift`/`enablebinary`/`disablebinary` via JMX, asserting that transport status is correct and no NPE is thrown before or after server startup.
