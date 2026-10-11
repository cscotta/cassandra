# Bug 2952: Bootstrap gossip state delete silently has no effect at protocol level

**Commit**: 0287d0dd87 · **Ticket**: n/a

## Detection Rule
When gossip application state is removed locally via `deleteApplicationState()` to signal a mode transition, check whether gossip actually propagates state deletions — if the protocol has no concept of deletion, the state will never be cleared on remote nodes.

## Code Smell
Code calls `epState.deleteApplicationState(BOOTSTRAP_MODE)` or similar to mark a node as done with bootstrap, but the `Gossiper` protocol only propagates state additions/updates; local deletes are never sent to peers, leaving remote nodes with stale bootstrap state.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: gossip-delete-no-propagation
- severity: silent-wrong-result
- subsystem: gossip

## Root Cause
Two interrelated bugs: (1) Token and bootstrap state are gossiped as separate application states, so it was possible to receive a token update without yet having seen the bootstrap state update, causing `TokenMetadata.bootstrapNodes` to miss the bootstrapping notice. (2) The code tried to signal the end of bootstrap by deleting the `BOOTSTRAP_MODE` application state locally, but gossip has no protocol-level concept of deletion — only additions/updates are propagated. The fix introduces a single `MODE` state (either `MODE_MOVING` or `MODE_NORMAL`) that transitions via an update (not a delete), ensuring all peers see the transition.

## Example
```java
// BEFORE: delete never propagates to other nodes
epState.deleteApplicationState(BOOTSTRAP_MODE);
// peers never learn bootstrap is done
```
```java
// AFTER: transition via update that gossip does propagate
Gossiper.instance().addApplicationState(StorageService.MODE,
    new ApplicationState(StorageService.MODE_NORMAL));
```
