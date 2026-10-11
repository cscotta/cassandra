# Bug 1255: ReconnectableSnitch Skips Reconnect During Rolling Upgrade Due to Version Guard

**Commit**: a7cae3255c · **Ticket**: CASSANDRA-6702
**Subsystem**: src/java/org/apache/cassandra/locator/ReconnectableSnitchHelper.java
**Category**: Logic error in condition

---

## Root Cause

`ReconnectableSnitchHelper.reconnect()` contained an extra condition requiring that the remote peer's messaging version equal `MessagingService.current_version` before reconnecting to the peer's local (private) address. During a rolling upgrade, a node that had not yet been upgraded would report an older messaging version, causing the snitch to skip the reconnect. As a result, intra-DC traffic continued to flow over the public address instead of the local address for the duration of the upgrade. The fix removes that version guard; the messaging layer already handles version negotiation separately, so the reconnect decision should depend only on whether the peer is in the same DC and is not already connected through the correct address.

### Before / After

```java
// BEFORE – version check prevents reconnect during upgrade
if (snitch.getDatacenter(publicAddress).equals(localDc)
        && MessagingService.instance().getVersion(publicAddress) == MessagingService.current_version
        && !MessagingService.instance().getConnectionPool(publicAddress).endPoint().equals(localAddress))
```

```java
// AFTER – reconnect whenever DC matches and address is wrong
if (snitch.getDatacenter(publicAddress).equals(localDc)
        && !MessagingService.instance().getConnectionPool(publicAddress).endPoint().equals(localAddress))
```

---

## How to Spot

Guards that require a peer to be at the current messaging version before performing a network-topology adjustment will silently misbehave during rolling upgrades, exactly the scenario where topology corrections are most critical. Any condition combining a version equality check with a connection management action should be scrutinized: ask whether the version check is truly a prerequisite or whether the messaging layer's own handshake already handles version differences.

---

## Test

No test added.
