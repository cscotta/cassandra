# Bug 1135: Silent Null Return in MerkleTrees.getMerkleTree Hides Missing Tree

**Commit**: 2a656e6bd5 · **Ticket**: CASSANDRA-5220
**Subsystem**: src/java/org/apache/cassandra/utils/MerkleTrees.java
**Category**: Missing null / bounds check

---

## Root Cause

`MerkleTrees.getMerkleTree(Token t)` iterated the known token ranges and returned `null` when no matching tree was found. All callers that subsequently dereferenced the returned tree would throw a `NullPointerException` far from the actual site of failure, making the defect hard to diagnose. Because every valid token must map to exactly one Merkle tree during a repair session, the absence of a matching tree is an invariant violation, not a recoverable condition. The fix replaces the silent `return null` with `throw new AssertionError`, surfacing the violation immediately with a descriptive message.

### Before / After

```java
// BEFORE: returns null when no tree found; callers NPE silently
private MerkleTree getMerkleTree(Token t)
{
    for (Map.Entry<Range<Token>, MerkleTree> entry : merkleTrees.entrySet())
        if (entry.getKey().contains(t))
            return merkleTrees.get(range);

    return null;
}
```

```java
// AFTER: asserts the invariant explicitly
private MerkleTree getMerkleTree(Token t)
{
    for (Map.Entry<Range<Token>, MerkleTree> entry : merkleTrees.entrySet())
        if (entry.getKey().contains(t))
            return merkleTrees.get(range);

    throw new AssertionError("Expected tree for token " + t);
}
```

---

## How to Spot

When a lookup method over a collection of domain objects returns `null` on no-match, ask whether the caller is ever supposed to handle a missing result or whether the no-match case is a logic error. If every valid input must produce a result (as in a partitioned data structure that covers the full token ring), a missing result is an invariant violation and should be reported as close to the source as possible. Null returns from internal helper methods that are only called with supposedly valid tokens are a common Coverity/static analysis flag; treat them as `AssertionError` or `IllegalStateException` rather than silent misses.

---

## Test

No new test added; the change is an assertion strengthening. The `LocalSyncTaskTest` had two unused import statements removed as a cleanup.
