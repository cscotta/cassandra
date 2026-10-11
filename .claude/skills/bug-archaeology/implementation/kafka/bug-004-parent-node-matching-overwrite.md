**Commit**: db3ece1042 · **Ticket**: KAFKA-20264
**Subsystem**: streams — InternalStreamsBuilder / topology optimization
**Category**: Logic error in condition

**Root cause**
`findParentNodeMatching` traversed the parent-node graph recursively looking for a node that satisfies a predicate. The loop assigned the recursive result unconditionally to `foundParentNode`, so a later sibling branch returning `null` would overwrite a valid match found by an earlier branch. The final `return foundParentNode` could then return `null` even though a matching ancestor existed. This broke repartition-topic sharing when two merged streams had key-changing operations on separate branches.

**Before/After**
```java
// Before — later null overwrites earlier match
for (final GraphNode parentNode : startSeekingNode.parentNodes()) {
    if (parentNodePredicate.test(parentNode)) { return parentNode; }
    foundParentNode = findParentNodeMatching(parentNode, parentNodePredicate);  // BUG
}
return foundParentNode;

// After — return immediately on first non-null result
for (final GraphNode parentNode : startSeekingNode.parentNodes()) {
    if (parentNodePredicate.test(parentNode)) { return parentNode; }
    final GraphNode foundParentNode = findParentNodeMatching(parentNode, parentNodePredicate);
    if (foundParentNode != null) { return foundParentNode; }
}
return null;
```

**How to spot**
Recursive graph-search functions that accumulate a result variable inside a loop and only check it at the end are prone to this pattern. The correct idiom for "find first match in a tree" is to return immediately on any non-null recursive result. Review any DFS/BFS-style traversal where the result variable is assigned (not compared) inside the loop.

**Test**
`InternalStreamsBuilderTest#shouldFindMatchingAncestorWhenLaterParentHasNoMatch` — constructs a diamond-shaped graph where the matching ancestor is reachable only from the first parent branch, then asserts the result is non-null. Two topology-optimization tests (`shouldShareRepartitionTopicForMergedStream*`) assert a single repartition node for merged streams with key-changing ops on each branch.
