# Bug 2853: Gossip quarantine cleanup runs inside wrong code block due to brace placement

**Commit**: 35f496d944 · **Ticket**: CASSANDRA-1670

## Detection Rule
When a block of code should execute unconditionally (or under a separate condition) but a missing or misplaced closing brace makes it a nested block of an unrelated `if`, the code runs only when the outer condition is true.

## Code Smell
`justRemovedEndpoints_` cleanup was indented inside the `for (InetAddress endpoint : liveMembers)` block — effectively running only when iterating live members, and only for the last endpoint in the loop. A closing brace in the wrong place is the entire cause.

## Tags
- scope: java
- category: logic-error
- code-pattern: misplaced-closing-brace
- severity: silent-wrong-result
- subsystem: gossip

## Root Cause
The `justRemovedEndpoints_` expiry loop was nested inside the `for (endpoint : liveMembers)` loop body due to an indentation/brace mistake. It should execute once per gossip round at the top level of `doGossipToLiveMember`. Nodes removed via `removetoken` were never fully graduated out of quarantine, preventing re-use of their slots in 2-node clusters.

## Example
```java
// BEFORE (justRemovedEndpoints cleanup nested inside liveMembers loop)
for (InetAddress endpoint : liveMembers) {
    // ...
    if (!justRemovedEndpoints_.isEmpty()) {
        for (entry : copy.entrySet()) { ... }
    }
}  // cleanup only ran during live member iteration

// AFTER (cleanup at correct scope)
for (InetAddress endpoint : liveMembers) { ... }
if (!justRemovedEndpoints_.isEmpty()) {
    for (entry : copy.entrySet()) { ... }
}
```
