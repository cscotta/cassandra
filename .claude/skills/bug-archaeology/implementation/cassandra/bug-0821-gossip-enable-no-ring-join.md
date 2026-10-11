# Bug 0821: Exceptions When Enabling Gossip on Nodes That Haven't Joined the Ring

**Commit**: df819ec833 · **Ticket**: CASSANDRA-12253
**Subsystem**: Gossip / Node Lifecycle
**Category**: Logic error in condition

---

## Root Cause
Two related problems existed when gossip was enabled on a node that had not yet joined the token ring (e.g., `cassandra.join_ring=false`). First, `StorageService.startGossiping()` unconditionally called `setGossipTokens(getLocalTokens())`, which failed with an assertion when no tokens had been saved. Second, `Gossiper.stop()` announced a shutdown for every node regardless of whether it had ever joined, which also produced exceptions because the node had no valid ring state to announce. The fix makes `startGossiping()` check for valid saved tokens before calling `setGossipTokens()` and adds an `isJoined()` guard to `Gossiper.stop()` before broadcasting the shutdown announcement.

### Before / After
```java
// BEFORE — startGossiping() always calls setGossipTokens
setGossipTokens(getLocalTokens());

// AFTER — only when tokens exist
Collection<Token> tokens = SystemKeyspace.getSavedTokens();
boolean validTokens = tokens != null && !tokens.isEmpty();
if (joined || joinRing)
    assert validTokens : "Cannot start gossiping for a node intended to join without valid tokens";
if (validTokens)
    setGossipTokens(tokens);

// BEFORE — Gossiper.stop() announces shutdown unconditionally
if (mystate != null && !isSilentShutdownState(mystate))

// AFTER — also requires node to have joined
if (mystate != null && !isSilentShutdownState(mystate) && StorageService.instance.isJoined())
```

---

## How to Spot
Any code path that operates on ring/token state should be guarded by `isJoined()` or a null/empty token check. Review all callers that assume tokens are present after `clientInitialization()` or early-startup — the `join_ring=false` case is a legitimate operating mode that must be handled gracefully throughout the gossip lifecycle.

---

## Test
No new test was added.
