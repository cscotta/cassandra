# Bug 0148: Gossip Status Not Updated After Host Replacement

**Commit**: 51ef21b6bc · **Ticket**: CASSANDRA-19712
**Subsystem**: tcm/listeners (LegacyStateListener)
**Category**: State not cleaned up

---

## Root Cause

When a node is replaced (TCM `NodeReplacementExecution`), `LegacyStateListener` updates token metadata to transfer ownership from the replaced node to the replacement. However, the listener omitted the call to `Gossiper.instance.mergeNodeToGossip(change, next, tokens)`, which is responsible for publishing the replacement node's `STATUS_WITH_PORT=NORMAL` gossip state to the rest of the cluster. Without this call the gossip table on peer nodes continued to show the replacement node in a non-NORMAL state, causing incorrect routing decisions and confusing operators monitoring cluster health.

### Before / After

```java
// BEFORE – gossip state never updated for the replacement node
for (Token token : tokens)
    logger.warn("Token {} changing ownership from {} to {}", token, replaced, replacement);
```

```java
// AFTER – gossip state propagated after logging
for (Token token : tokens)
    logger.warn("Token {} changing ownership from {} to {}", token, replaced, replacement);
Gossiper.instance.mergeNodeToGossip(change, next, tokens);
```

---

## How to Spot

Whenever a TCM change listener handles a topology transition (join, replace, decommission, move) it must call the corresponding Gossip synchronization helper to keep the legacy gossip view consistent with the TCM state. Missing a `mergeNodeToGossip` or equivalent call in any branch of a `ChangeListener` will silently diverge the gossip state from the authoritative TCM state.

---

## Test

Integration test verifies that after a replacement operation completes, `Gossiper.getApplicationState(replacement, STATUS_WITH_PORT)` returns a value starting with `"NORMAL"` within 20 seconds on both the seed node and the replacement node.
