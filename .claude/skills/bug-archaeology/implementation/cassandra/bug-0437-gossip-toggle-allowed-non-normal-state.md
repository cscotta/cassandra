# Bug 0437: Gossip Enable/Disable Allowed During Non-NORMAL States

**Commit**: fee7a10823 · **Ticket**: CASSANDRA-16146
**Subsystem**: `src/java/org/apache/cassandra/service/StorageService.java`
**Category**: Logic error in condition

---

## Root Cause

`StorageService.stopGossiping()` and `startGossiping()` had no guard against being called while the node was in a transitional state such as BOOTSTRAPPING or MOVING. Disabling gossip mid-bootstrap could leave the cluster unable to converge on ring membership, and re-enabling gossip during MOVING could corrupt token ownership state. The pre-existing shutdown guard in `checkServiceAllowedToStart` only blocked the DRAINING/DRAINED state and was not present in `stopGossiping` at all, so both operations were silently permitted in any non-NORMAL state.

### Before / After

```java
// BEFORE – stopGossiping had no state check
public void stopGossiping() {
    if (initialized) {
        logger.warn("Stopping gossip by operator request");
        // ...
    }
}
```

```java
// AFTER – stopGossiping and startGossiping both require NORMAL state
public void stopGossiping() {
    if (initialized) {
        if (!isNormal())
            throw new IllegalStateException(
                "Unable to stop gossip because the node is not in the normal state. " +
                "Try to stop the node instead.");
        // ...
    }
}
```

---

## How to Spot

Any administrative operation that mutates cluster membership or communication state (gossip, native transport, drain) should be audited for a state-machine guard at the entry point. The pattern to look for: a method that calls `Gossiper.instance.stop()` or `start()` without first asserting `operationMode == Mode.NORMAL`. Similarly, look for `decommission`-style preconditions (`operationMode != Mode.NORMAL`) that compare against the raw field — these are candidates to be replaced with a shared `isNormal()` helper to avoid drift.

---

## Test

`testPreventStoppingGossipDuringBootstrap` intercepts the bootstrap flow with ByteBuddy to pause it mid-flight, then calls `stopGossiping()` and asserts an `IllegalStateException` is thrown. `testPreventEnablingGossipDuringMove` sets the node to MOVING mode and asserts `startGossiping()` also throws.
