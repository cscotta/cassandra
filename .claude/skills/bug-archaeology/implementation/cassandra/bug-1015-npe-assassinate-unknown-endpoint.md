# Bug 1015: NPE assassinating unknown endpoint — tokens fetched before gossip sleep

**Commit**: b89037fe40 · **Ticket**: CASSANDRA-9510
**Subsystem**: Gossip / Failure detection
**Category**: Missing null / bounds check

---

## Root Cause
`Gossiper.assassinateEndpoint()` must mark a node as LEFT, which requires obtaining the node's tokens.  The original code fetched the tokens inside a `try/catch` block positioned *before* the gossip sleep and heartbeat-generation bump.  If the endpoint was completely unknown to token metadata (e.g. a node that never fully joined), `getTokens()` threw and the catch block fell back to a random token.  However the `tokens` variable was only in scope inside that early try/catch, so the subsequent `epState.addApplicationState(STATUS, valueFactory.left(tokens, ...))` call further down referenced `tokens` from the outer scope, which was still `null` — causing a `NullPointerException`.  The fix moves the token-fetching block to *after* the sleep and heartbeat update so that `tokens` is assigned exactly once and is always non-null when `addApplicationState` is called.

### Before / After
```java
// BEFORE – tokens assigned inside early try/catch; outer tokens variable stays null
try {
    tokens = StorageService.instance.getTokenMetadata().getTokens(endpoint);
} catch (Throwable th) {
    ...
    tokens = Collections.singletonList(...randomToken...);
}
// ... sleep, heartbeat bump ...
// tokens may still be the outer-scope null if the above try/catch was the wrong branch
epState.addApplicationState(ApplicationState.STATUS,
    StorageService.instance.valueFactory.left(tokens, computeExpireTime())); // NPE

// AFTER – token-fetching moved after the sleep; always non-null before use
// ... sleep, heartbeat bump ...
try {
    tokens = StorageService.instance.getTokenMetadata().getTokens(endpoint);
} catch (Throwable th) {
    ...
    tokens = Collections.singletonList(...randomToken...);
}
epState.addApplicationState(ApplicationState.STATUS,
    StorageService.instance.valueFactory.left(tokens, computeExpireTime())); // safe
```

---

## How to Spot
Variable initialization inside a try/catch that is not the final assignment before use is a classic NPE source.  When the catch re-assigns the variable to a fallback, double-check that no code path can read the variable before the try/catch executes.  In this case the outer-scope declaration `tokens = null` made it possible to reach the `addApplicationState` call with a null value if the control flow was restructured.

---

## Test
No test added for this error path.
