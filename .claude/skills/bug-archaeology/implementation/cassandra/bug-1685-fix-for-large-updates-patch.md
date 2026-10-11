# Bug 1685: Fix BTree.clear for Large Updates

**Commit**: e6e596d1a8 · **Ticket**: CASSANDRA-6943
**Subsystem**: src/java/org/apache/cassandra/utils/btree
**Category**: State not cleaned up

---

## Root Cause

`NodeBuilder` instances are organized as a doubly-linked list of parent/child nodes reused across BTree update operations. When an update is aborted early (via `UpdateFunction.abortEarly()`), `NodeBuilder.clear()` walked only the `child` chain, ignoring any `parent` nodes that had already been activated during the partial update. Those parent `NodeBuilder` entries were left with stale `upperBound` state, causing the next update invocation to read garbage state from the dirty parent nodes and produce a malformed tree.

### Before / After

```java
// BEFORE: only walks child chain
void clear()
{
    NodeBuilder current = this;
    while (current != null)
    {
        if (current.upperBound != null)
        {
            current.reset(null, null, null, null);
            Arrays.fill(current.buildKeys, 0, current.maxBuildKeyPosition, null);
            Arrays.fill(current.buildChildren, 0, current.maxBuildKeyPosition + 1, null);
            current.maxBuildKeyPosition = 0;
        }
        current = current.child;
    }
}
```

```java
// AFTER: walks both child and parent chains
void clear()
{
    NodeBuilder current = this;
    while (current != null && current.upperBound != null)
    {
        current.clearSelf();
        current = current.child;
    }
    current = parent;
    while (current != null && current.upperBound != null)
    {
        current.clearSelf();
        current = current.parent;
    }
}
```

---

## How to Spot

Any data structure that walks a linked list for cleanup must walk all reachable directions. A doubly-linked node builder that only clears in one direction on abort will leave the opposite chain dirty. Whenever `clear()` or `reset()` walks only `child`, check whether a `parent` pointer also needs to be traversed.

---

## Test

`BTreeTest.testClearOnAbort()` was added. It builds a small BTree, performs an update that aborts after 90 steps against a larger input, then performs a full update and asserts the resulting tree is well-formed. This directly reproduces the corruption caused by the incomplete parent-chain cleanup.
