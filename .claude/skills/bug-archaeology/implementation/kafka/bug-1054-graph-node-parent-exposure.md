## Commit

e88c10d595 — KAFKA-4650: GraphNode.parentNodes() exposes mutable internal set

## Ticket

KAFKA-4650

## Subsystem

Kafka Streams / processor topology graph

## Category

Missing null / bounds check

## Root cause

`GraphNode.children()` returned a defensive copy of the internal `LinkedHashSet`, but `parentNodes()` returned the live internal set directly. Callers that received the return value of `parentNodes()` could accidentally mutate the node's parent set, corrupting the topology graph without any visible error. The asymmetry between the two accessors was easy to overlook and led to subtle topology-ordering bugs when graph traversal code modified the returned collection.

## Before / After

```java
// Before — returns live internal set
public LinkedHashSet<GraphNode<K, V>> parentNodes() {
    return parentNodes;
}

// After — returns defensive copy, consistent with children()
public LinkedHashSet<GraphNode<K, V>> parentNodes() {
    return new LinkedHashSet<>(parentNodes);
}
```

## How to spot

Review every accessor in graph or node classes that returns a collection field. If `children()` makes a copy but `parentNodes()` does not (or vice versa), the asymmetry is a likely encapsulation gap that can lead to silent external mutation.

## Test

`GraphNodeTest` was added to verify that modifications to the collection returned by `parentNodes()` do not affect the node's internal parent set.
