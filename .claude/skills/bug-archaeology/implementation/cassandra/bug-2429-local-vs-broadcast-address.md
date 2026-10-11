# Bug 2429: Local address used instead of broadcast address for self-identity checks

**Commit**: 089eacf600 · **Ticket**: CASSANDRA-3835

## Detection Rule

When a distributed node must identify itself by address (e.g., gossip deduplication, token ownership), verify that every comparison uses the externally-visible broadcast address, not the local bind address.

## Code Smell

Code compares an endpoint address with `FBUtilities.getLocalAddress()` or `InetAddress.getLocalHost()` where `FBUtilities.getBroadcastAddress()` is required; in multi-region/EC2 deployments the two differ, making the self-identity check fail silently.

## Tags

- scope: cassandra
- category: wrong-constant
- code-pattern: local-vs-broadcast-address
- severity: silent-wrong-result
- subsystem: gossip

## Root Cause

Several code paths used `FBUtilities.getLocalAddress()` (the bind address) or `InetAddress.getLocalHost()` to detect "am I the node in question?" In EC2 multi-region setups the bind address is a private IP while the broadcast address is the public IP. Using the wrong address caused nodes to not recognise themselves, resulting in incorrect gossip state (endpoints added as remote peers instead of being skipped) and wrong replication strategy options in CLI.

## Example

```java
// BEFORE – wrong address in self-check
if (ep == FBUtilities.getLocalAddress()) {
    logger.debug("Attempt to add self as saved endpoint");
    return;
}

// AFTER
if (ep == FBUtilities.getBroadcastAddress()) {
    logger.debug("Attempt to add self as saved endpoint");
    return;
}
```
