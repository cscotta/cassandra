# Bug 0194: nodetool enablebinary/disablebinary Does Not Update RPC Readiness in Gossip

**Commit**: b51ee83a29 · **Ticket**: CASSANDRA-18935
**Subsystem**: service/CassandraDaemon
**Category**: State not cleaned up

---

## Root Cause

`setRpcReady(true)` was called inside the startup path's conditional block in `setup()`, so it only fired when native transport was started as part of initial node startup. When `startNativeTransport()` was called later (e.g. via `nodetool enablebinary`), gossip was never told that the native transport became ready. Symmetrically, `stopNativeTransport()` never called `setRpcReady(false)`, so gossip continued advertising the node as RPC-ready after `nodetool disablebinary`. Other nodes therefore kept routing client traffic to a node that was no longer accepting it.

### Before / After

```java
// BEFORE – setRpcReady only in startup path
if (...startNativeTransport...) {
    startNativeTransport();
    StorageService.instance.setRpcReady(true);  // never called from JMX/nodetool path
}

public void stopNativeTransport() {
    if (nativeTransportService != null)
        nativeTransportService.stop();  // no setRpcReady(false)
}
```

```java
// AFTER – gossip updated inside startNativeTransport/stopNativeTransport
public void startNativeTransport() {
    boolean alreadyRunning = nativeTransportService.isRunning();
    nativeTransportService.start();
    if (!alreadyRunning)
        StorageService.instance.setRpcReady(true);
}

public void stopNativeTransport() {
    if (nativeTransportService != null) {
        StorageService.instance.setRpcReady(false);
        nativeTransportService.stop();
    }
}
```

---

## How to Spot

Whenever a system has both an "initial startup" code path and an "operator-triggered" code path that invoke the same underlying operation, side effects (especially external state like gossip readiness flags) must live inside the shared operation method rather than only at the call site in the startup path. Any separate invocation of the underlying operation will silently skip the side effect.

---

## Test

`NativeProtocolTest.testBinaryReflectsRpcReadiness` starts a single-node cluster with native transport disabled, verifies `isRpcReady` is false, enables transport, verifies it becomes true, disables transport, and verifies it returns to false.
