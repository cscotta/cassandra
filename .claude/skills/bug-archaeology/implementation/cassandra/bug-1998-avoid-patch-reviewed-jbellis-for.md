# Bug 1998: EC2MultiRegionSnitch Reconnects Even When Already on the Correct Internal IP

**Commit**: 998fe96766 · **Ticket**: CASSANDRA-5678
**Subsystem**: src/java/org/apache/cassandra/locator/Ec2MultiRegionSnitch.java, src/java/org/apache/cassandra/net/OutboundTcpConnectionPool.java
**Category**: Logic error in condition

---

## Root Cause

`Ec2MultiRegionSnitch.reConnect()` was supposed to switch an outbound connection from a node's public IP to its internal (same-DC) IP when both nodes are in the same datacenter and messaging versions are compatible. However, the condition never checked whether the connection pool was already pointing at the internal IP. Every time a gossip `STATUS` update arrived for that endpoint, `reset()` was called unconditionally, tearing down and re-establishing the connection even when it was already correct. The fix adds a check that the current pool endpoint differs from the target internal IP before resetting.

### Before / After

```java
// BEFORE – reconnects every gossip heartbeat
if (getDatacenter(endpoint).equals(getDatacenter(public_ip))
    && MessagingService.instance().getVersion(endpoint) == MessagingService.current_version)
{
    InetAddress remoteIP = InetAddress.getByName(versionedValue.value);
    MessagingService.instance().getConnectionPool(endpoint).reset(remoteIP);
}
```

```java
// AFTER – reconnects only when the endpoint has actually changed
InetAddress localEc2IP = InetAddress.getByName(versionedValue.value);
if (getDatacenter(endpoint).equals(getDatacenter(public_ip))
    && MessagingService.instance().getVersion(endpoint) == MessagingService.current_version
    && !MessagingService.instance().getConnectionPool(endpoint).endPoint().equals(localEc2IP))
{
    MessagingService.instance().getConnectionPool(endpoint).reset(localEc2IP);
}
```

---

## How to Spot

Mutations that change shared networking state (connection resets, reconnects) must be guarded by an idempotency check: "is the current state already the desired state?" Any code that reacts to gossip or periodic state-change events without such a guard will perform redundant and potentially disruptive operations on every heartbeat cycle.

---

## Test

No test added.
