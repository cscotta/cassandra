# Bug 2278: Bootstrap Uses Gossip Schema Check That Requires Sleep, Causes False Instajoin

**Commit**: 988c4132d7 · **Ticket**: CASSANDRA-4427

## Detection Rule

When bootstrap logic decides whether to join an existing cluster versus start fresh, verify that the discriminating signal (schema presence, seed membership) does not require a fixed sleep to become valid, otherwise new nodes may incorrectly skip bootstrap.

## Code Smell

Bootstrap decision code sleeps a fixed interval and then checks gossip for a schema version, but gossip may not have propagated yet; the sleep duration is a guess, and nodes that are seeds would still enter the bootstrap path unnecessarily.

## Tags

- scope: cassandra
- category: race-condition
- code-pattern: gossip-schema-timing-window
- severity: data-loss
- subsystem: bootstrap-startup

## Root Cause

The original bootstrap gate checked whether any peer gossip state contained a non-empty schema version after a fixed 2×`RING_DELAY` sleep. If gossip had not yet propagated, the check would return no schema and the node would skip bootstrap — joining as if it were a brand-new cluster even when rejoining an existing one. The fix replaces the gossip schema check with a seed-list check: a node skips bootstrap only if it is itself a configured seed, which is a locally available fact requiring no sleep or gossip.

## Example

```java
// BEFORE — needs sleep + gossip to work reliably
FBUtilities.sleep(2 * Gossiper.intervalInMillis);
for (Entry<InetAddress, EndpointState> entry : Gossiper.instance.getEndpointStates()) {
    VersionedValue schemaValue = entry.getValue().getApplicationState(ApplicationState.SCHEMA);
    if (schemaValue != null && !schemaValue.value.equals(Schema.emptyVersion.toString()))
        schemaPresent = true;
}
if (isAutoBootstrap && (bootstrapInProgress || (!bootstrapComplete && schemaPresent)))
    doBootstrap();
```

```java
// AFTER — seed check is local, no sleep needed
if (isAutoBootstrap
    && !SystemTable.bootstrapComplete()
    && !DatabaseDescriptor.getSeeds().contains(FBUtilities.getBroadcastAddress()))
    doBootstrap();
```
