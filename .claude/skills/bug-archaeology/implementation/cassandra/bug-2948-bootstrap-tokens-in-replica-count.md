# Bug 2948: Bootstrap tokens counted as real ring members, skewing replication

**Commit**: b3b58932f3 · **Ticket**: CASSANDRA-525

## Detection Rule
When a replication strategy iterates the token ring to find replica endpoints, check whether bootstrapping nodes (which have not yet received data) are included in the count — they should not satisfy the replica count requirement.

## Code Smell
`RackUnawareStrategy.getNaturalEndpoints` used a separate `foundCount` variable that only incremented for non-bootstrap tokens while still tracking all tokens in `tokenList`; this made the termination condition inconsistent and allowed bootstrap tokens to satisfy the replica count early.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: bootstrap-tokens-in-replica-count
- severity: silent-wrong-result
- subsystem: bootstrap-startup

## Root Cause
The old code maintained `bsTokens` (bootstrap tokens) to avoid counting them toward `foundCount`, but still added them to `tokenList` and used `tokenList.contains` checks. The invariant between `foundCount` and `tokenList.size()` was broken — `tokenList` could hold bootstrap tokens that did not increment `foundCount`, causing the loop to over-iterate or under-count real replicas. The fix removes bootstrap tokens from the normal ring entirely (handled by commit 2947), so the natural-endpoint calculation can simply use `tokenList.size() < replicas_` as the termination condition with no special case needed.

## Example
```java
// BEFORE: separate foundCount vs tokenList size, bootstrap special-casing
for (int i = startIndex, count = 1; count < totalNodes && foundCount < replicas_; ...) {
    if (!tokenList.contains(tokens.get(i))) {
        tokenList.add(tokens.get(i));
        if (bsTokens == null || !bsTokens.contains(tokens.get(i)))
            foundCount++;
    }
}
```
```java
// AFTER: bootstrap tokens not in ring, single clean condition
for (int i = startIndex, count = 1; count < totalNodes && tokenList.size() < replicas_; ...) {
    assert !tokenList.contains(tokens.get(i));
    tokenList.add((Token) tokens.get(i));
}
```
