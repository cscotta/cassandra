# Bug 0815: TokenAllocation uses live token metadata for strategy, causing exception during vnode allocation

**Commit**: 85ac06c640 · **Ticket**: CASSANDRA-12715
**Subsystem**: Token Allocation / DHT
**Category**: Logic error in condition

---

## Root Cause
`TokenAllocation.allocateTokens()` passed the live `tokenMetadata` directly to `getStrategy()`, which builds a replication-strategy adapter, and then also used the same live object for `replicatedOwnershipStats()`. When new tokens were inserted into a cloned copy for statistics comparison, the sequence was: build strategy from live metadata → compute pre-allocation stats from live metadata → clone → insert → compute post-allocation stats from clone. The mismatch between the strategy's view of the ring (built from live metadata without the new tokens) and the cloned metadata (with the tokens) could produce exceptions when the strategy tried to enumerate replicas for the new positions. The fix builds `tokenMetadataCopy` before constructing the strategy, uses the copy for both the strategy and the pre-allocation stats, and only inserts tokens into the copy for the post-allocation stats.

### Before / After
```java
// BEFORE — strategy built from live metadata, clone created later
StrategyAdapter strategy = getStrategy(tokenMetadata, rs, endpoint);
Collection<Token> tokens = create(tokenMetadata, strategy).addUnit(endpoint, numTokens);
...
SummaryStatistics os = replicatedOwnershipStats(tokenMetadata, rs, endpoint);
TokenMetadata tokenMetadataCopy = tokenMetadata.cloneOnlyTokenMap();

// AFTER — clone first, use consistently
TokenMetadata tokenMetadataCopy = tokenMetadata.cloneOnlyTokenMap();
StrategyAdapter strategy = getStrategy(tokenMetadataCopy, rs, endpoint);
Collection<Token> tokens = create(tokenMetadata, strategy).addUnit(endpoint, numTokens);
...
SummaryStatistics os = replicatedOwnershipStats(tokenMetadataCopy, rs, endpoint);
tokenMetadataCopy.updateNormalTokens(tokens, endpoint);
```

---

## How to Spot
Whenever code creates a clone of a mutable object partway through a method but passes the original to earlier helpers, the helpers and the clone may see an inconsistent ring state. During review, verify that any `cloneOnlyTokenMap()` (or similar snapshot) call appears before — not after — the helpers that should operate on it.

---

## Test
`BootStrapperTest` was updated to exercise the corrected allocation path against both Murmur3 and Random partitioners with multi-vnode configurations.
