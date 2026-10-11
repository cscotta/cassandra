# Bug 2471/2472: Gossiper adds itself to saved endpoints, causing self-entry in system token table

**Commit**: 96d92fb5d9 (original fix) / 27b733efe2 (follow-up fix for wrong address method) · **Ticket**: CASSANDRA-3485

## Detection Rule

When a self-identity guard uses `getBroadcastAddress()` but the node was initialized with `getLocalAddress()`, the comparison will be wrong if broadcast and local addresses differ (e.g., NAT/multi-homed).

## Code Smell

Identity comparison `ep == FBUtilities.getBroadcastAddress()` used as self-check guard in methods that historically used `getLocalAddress()` for self-identification, or vice-versa; the two methods may return different objects/values.

## Tags

- scope: cassandra
- category: wrong-constant
- code-pattern: wrong-self-address-method
- severity: silent-wrong-result
- subsystem: gossip

## Root Cause

`Gossiper.addSavedEndpoint()` and `SystemTable.updateToken()` lacked guards to skip self. The initial fix (2472) added guards using `getBroadcastAddress()` but commit 2471 corrected them to use `getLocalAddress()`, the address actually used for identity comparisons in that subsystem. Without the guard, the node would record its own address/token into the system peers table, corrupting ring state on restart.

## Example

```java
// BEFORE (missing guard entirely)
public void addSavedEndpoint(InetAddress ep)
{
    EndpointState epState = new EndpointState(new HeartBeatState(0));
    ...
}
```

```java
// AFTER (correct guard)
public void addSavedEndpoint(InetAddress ep)
{
    if (ep == FBUtilities.getLocalAddress())
    {
        logger.debug("Attempt to add self as saved endpoint");
        return;
    }
    ...
}
```
