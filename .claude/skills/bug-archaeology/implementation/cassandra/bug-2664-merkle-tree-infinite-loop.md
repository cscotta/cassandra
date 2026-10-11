# Bug 2664: Infinite loop when initializing a merkle tree

**Commit**: 203e2c2d90 · **Ticket**: CASSANDRA-2758

## Detection Rule
When a recursive tree-building function checks a boundary condition on only one child at a time rather than short-circuiting for both, it can loop infinitely when the midpoint collapses to an endpoint.

## Code Smell
A recursive `initHelper` that checks `midpoint.equals(left)` and `midpoint.equals(right)` independently on each recursive call, creating a left child with `left==midpoint` and a right child with `midpoint==right` without ever stopping.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: recursive-boundary-not-checked
- severity: hang
- subsystem: repair

## Root Cause
When the token space is exhausted (the midpoint equals one of the bounds), each per-child guard only prevented *that* branch from recursing further but still constructed an `Inner` node that forced the other branch to recurse indefinitely. The fix checks whether the midpoint equals either bound *before* constructing any children, and returns a `Leaf` immediately in that case.

## Example
```java
// BEFORE
Token midpoint = partitioner.midpoint(left, right);
Hashable lchild = midpoint.equals(left)  ? new Leaf() : initHelper(left, midpoint, inc(depth), max);
Hashable rchild = midpoint.equals(right) ? new Leaf() : initHelper(midpoint, right, inc(depth), max);
return new Inner(midpoint, lchild, rchild);
```
```java
// AFTER
Token midpoint = partitioner.midpoint(left, right);
if (midpoint.equals(left) || midpoint.equals(right))
    return new Leaf();
Hashable lchild = initHelper(left, midpoint, inc(depth), max);
Hashable rchild = initHelper(midpoint, right, inc(depth), max);
return new Inner(midpoint, lchild, rchild);
```
