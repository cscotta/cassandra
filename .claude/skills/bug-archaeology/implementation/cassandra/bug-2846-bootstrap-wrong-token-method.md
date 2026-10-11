# Bug 2846: Bootstrap with no non-system tables calls wrong token registration method

**Commit**: 893809bdfb · **Ticket**: CASSANDRA-1732

## Detection Rule
When a node finishes bootstrapping (or skips bootstrap because no user tables exist), call `setToken(token)` — which handles ring registration, gossip, and notifyListeners — rather than calling `tokenMetadata_.updateNormalToken(token, ...)` directly, which bypasses those steps.

## Code Smell
`tokenMetadata_.updateNormalToken(token, FBUtilities.getLocalAddress())` updates only the in-memory token map without calling gossip state updates or notifying other subsystems. The node appears to join the ring locally but remote nodes do not learn of its token.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: wrong-token-registration-method
- severity: crash
- subsystem: bootstrap-startup

## Root Cause
When no non-system tables are defined, the bootstrap code path called `tokenMetadata_.updateNormalToken` instead of the higher-level `setToken`, which also gossips `STATE_NORMAL` and fires `IEndpointStateChangeSubscriber.onJoinRing`. Other nodes in the cluster would not see the bootstrapping node as a valid ring member.

## Example
```java
// BEFORE
isBootstrapMode = false;
SystemTable.setBootstrapped(true);
tokenMetadata_.updateNormalToken(token, FBUtilities.getLocalAddress());
Gossiper.instance.addLocalApplicationState(ApplicationState.STATUS, valueFactory.normal(token));

// AFTER
isBootstrapMode = false;
SystemTable.setBootstrapped(true);
setToken(token);
Gossiper.instance.addLocalApplicationState(ApplicationState.STATUS, valueFactory.normal(token));
```
