# Bug 1421: Fix Shutdown When Run as Windows Service

**Commit**: a29ee07e30 · **Ticket**: CASSANDRA-8136
**Subsystem**: service/CassandraDaemon
**Category**: Missing override of default method

---

## Root Cause

`CassandraDaemon.stop()` was written for Linux/JSVC, where the service supervisor (jsvc) calls `stop()` to halt the RPC servers and then handles full process teardown itself via OS signals. On Windows, the equivalent supervisor is Apache Procrun (prunsrv), which calls `stop()` but does not perform the subsequent JVM teardown that jsvc does. This meant that on Windows, calling `stop()` (e.g., via the service control manager) would halt Thrift and native transports but leave the JVM running indefinitely, making the service appear stopped to the OS while the process lingered. The fix detects non-Unix platforms after stopping the servers and calls `System.exit(0)` to trigger the shutdown hook and terminate the process.

### Before / After

```java
// BEFORE — stop() only shuts down RPC servers; relies on jsvc for full teardown
public void stop() {
    logger.info("Cassandra shutting down...");
    thriftServer.stop();
    nativeServer.stop();
}
```

```java
// AFTER — on Windows, force JVM exit after stopping servers
public void stop() {
    logger.info("Cassandra shutting down...");
    thriftServer.stop();
    nativeServer.stop();

    // On windows, we need to stop the entire system as prunsrv doesn't have the jsvc hooks
    if (!FBUtilities.isUnix())
        System.exit(0);
}
```

---

## How to Spot

When a lifecycle method (start, stop, destroy) was originally written for one platform or container and is later run under a different one, check whether the new environment provides the same teardown guarantees. In particular, look for implicit assumptions like "the supervisor will clean up after us" — if those assumptions are documented only in comments (or not at all), they are fragile. Platform-specific branches with clear comments explaining why they diverge are the correct pattern.

---

## Test

No test added.
