# Bug 2697: Merkle tree splitting exits early due to incorrect StopRecursion condition

**Commit**: d719e5787c · **Ticket**: CASSANDRA-2605

## Detection Rule
When splitting a merkle tree node, throw `StopRecursion.TooDeep` only when the computed midpoint would create a degenerate zero-width range (midpoint equals either boundary), not when the target token happens to equal the node's existing split token.

## Code Smell
`if (t.equals(node.token) || t.equals(pright)) throw new StopRecursion.TooDeep();` placed before the recursive descent, causing the split to abort early for valid tokens that just happen to equal an existing inner-node split point.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: wrong-early-termination-condition
- severity: silent-wrong-result
- subsystem: repair

## Root Cause
The tree-splitting helper threw `TooDeep` when the target token `t` equalled an existing inner-node's split token or the right boundary, even when further splitting was possible and necessary. This caused repair to use a coarser-than-needed tree, missing inconsistencies in token ranges that could have been split further. The fix moves the `TooDeep` guard to after the midpoint is computed, throwing only when `midpoint.equals(pleft) || midpoint.equals(pright)` (i.e., no further subdivision is possible), and removes the incorrect early-exit on `t.equals(node.token)`.

## Example
```java
// BEFORE (premature TooDeep before computing midpoint)
if (t.equals(node.token) || t.equals(pright))
    throw new StopRecursion.TooDeep();
// recurse on matching child
```
```java
// AFTER (TooDeep only when midpoint collapses)
Token midpoint = partitioner.midpoint(pleft, pright);
if (midpoint.equals(pleft) || midpoint.equals(pright))
    throw new StopRecursion.TooDeep();
// recurse on matching child
```
