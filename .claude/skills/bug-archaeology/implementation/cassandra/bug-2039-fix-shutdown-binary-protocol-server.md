# Bug 2039: Binary Protocol (Native Transport) Server Not Stopped During Drain and Shutdown

**Commit**: a6edfa5696 · **Ticket**: CASSANDRA-5507
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: State not cleaned up

---

## Root Cause

`StorageService.drain()` and the hard-shutdown path both called `stopRPCServer()` to stop the Thrift transport, but never called `stopNativeTransport()` for the binary protocol (CQL native) server. This left the native transport accepting new client connections during drain and continuing to run after a shutdown request, preventing a clean node stop for clients using the binary protocol. The fix introduces a `shutdownClientServers()` helper that calls both `stopRPCServer()` and `stopNativeTransport()`, and replaces every standalone `stopRPCServer()` call in the drain/shutdown paths with this helper.

### Before / After

```java
// BEFORE
stopRPCServer();
optionalTasks.shutdown();
```

```java
// AFTER
shutdownClientServers();  // stops both Thrift and native transport
optionalTasks.shutdown();
```

---

## How to Spot

Whenever a new server transport is added alongside an existing one, search for every call site that stops the existing transport and ensure the new transport is stopped in the same places. Omitting it from drain or shutdown paths is a common oversight.

---

## Test

No test added.
