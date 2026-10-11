# Bug 2524: Failed-to-join nodes stuck in JOINING state forever due to premature fat-client eviction

**Commit**: 755522d079 · **Ticket**: CASSANDRA-3351

## Detection Rule
When gossip considers evicting a silent node as a "fat client," verify that the node is not actually a member of the token ring before evicting it; otherwise a node that failed during bootstrap is permanently removed instead of being tracked for eventual eviction via the normal dead-node path.

## Code Smell
`if (!epState.hasToken() && !epState.isAlive() ...) { evictFromMembership(endpoint); }` executed before `setHasToken(true)` is called — the `isMember()` check that sets `hasToken` to `true` was placed outside the fat-client branch, so it always ran, causing nodes that were actually ring members to be incorrectly evicted via the fat-client path during gossip convergence.

## Tags
- scope: distributed-systems
- category: logic-error
- code-pattern: premature-eviction-wrong-condition-order
- severity: crash
- subsystem: gossip

## Root Cause
The gossip task ran `epState.setHasToken(true)` unconditionally whenever `isMember()` returned true, *before* the fat-client check. This meant `hasToken` was set to `true` before the check `!epState.hasToken()`, preventing legitimate fat-client eviction. Separately, a node that failed during bootstrap never got `hasToken` set (because it was never a full member), and `isDeadState()` would NPE or return wrong results if `ApplicationState.STATUS` was absent. The fix moves `setHasToken(true)` *inside* the fat-client candidate branch and guards it with an `else`-evict, and refactors `isDeadState()` to handle a null STATUS state gracefully.

## Example
```java
// BEFORE — setHasToken runs before fat-client check
if (StorageService.instance.getTokenMetadata().isMember(endpoint))
    epState.setHasToken(true);
if (!epState.hasToken() && !epState.isAlive() && duration > FatClientTimeout) {
    removeEndpoint(endpoint);
    evictFromMembership(endpoint);
}
```
```java
// AFTER — membership check inside fat-client candidate block
if (!epState.hasToken() && !epState.isAlive() && duration > FatClientTimeout) {
    if (StorageService.instance.getTokenMetadata().isMember(endpoint))
        epState.setHasToken(true);
    else {
        removeEndpoint(endpoint);
        evictFromMembership(endpoint);
    }
}
```
