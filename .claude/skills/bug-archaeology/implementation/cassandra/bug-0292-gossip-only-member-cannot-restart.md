# Bug 0292: Gossip-Only Member Cannot Restart Services After Stop

**Commit**: a7b53217bd · **Ticket**: CASSANDRA-17752
**Subsystem**: service / gossip
**Category**: Logic error in condition

---

## Root Cause

`StorageService.checkServiceAllowedToStart()` and `StorageService.stopGossiping()` both guarded their operation with `if (!isNormal())`, rejecting any action on a node not in `NORMAL` operation mode. A gossip-only member (started with `-Dcassandra.join_ring=false`) never transitions to `NORMAL`; it stays in `STARTING` state permanently. This meant that calling `stopGossiping()` on a gossip-only member threw `IllegalStateException("Unable to stop gossip because the node is not in the normal state")`, and then `startGossiping()` / `startNativeTransport()` / `startRPCServer()` also threw because `checkServiceAllowedToStart()` demanded `isNormal()` before allowing a restart.

### Before / After

```java
// BEFORE - rejects gossip-only members unconditionally
if (!isNormal())
    throw new IllegalStateException("Unable to stop gossip because the node is not in the normal state.");
// ...
if (!isNormal())
    throw new IllegalStateException("Unable to start " + service + " because the node is not in the normal state.");
```

```java
// AFTER - gossip-only members are exempt from the isNormal() requirement
if (!isNormal() && joinRing)
    throw new IllegalStateException("Unable to stop gossip because the node is not in the normal state. Try to stop the node instead.");
// ...
if (!isNormal() && joinRing) // if the node is not joining the ring, it is gossipping-only member which is in STARTING state forever
    throw new IllegalStateException("Unable to start " + service + " because the node is not in the normal state.");
```

---

## How to Spot

Guards of the form `!isNormal()` that protect restart/stop operations are appropriate for full ring members but become false positives for any special operating mode (gossip-only, client mode, read-only mode) where `NORMAL` is never entered. During review, whenever a new node boot mode is introduced, audit every `isNormal()` guard in `StorageService` to ensure the guard is combined with a mode-specific condition (`&& joinRing`, `&& !isClientMode()`, etc.) so that the new mode is not inadvertently blocked.

---

## Test

`GossipTest.restartGossipOnGossippingOnlyMember()` starts a gossip-only node, stops gossip, then restarts it and asserts `isGossipRunning()` returns true after the restart; equivalent tests in `NativeProtocolTest` and `ThriftTest` cover native transport and Thrift server restarts.
