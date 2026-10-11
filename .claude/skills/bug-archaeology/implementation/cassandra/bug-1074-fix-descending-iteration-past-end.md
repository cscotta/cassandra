# Bug 1074: Fix Descending Iteration Past End of BTreeSearchIterator

**Commit**: aef71696e6 · **Ticket**: CASSANDRA-10301
**Subsystem**: src/java/org/apache/cassandra/utils/btree/BTreeSearchIterator.java, src/java/org/apache/cassandra/utils/btree/TreeCursor.java
**Category**: Off-by-one

---

## Root Cause

`TreeCursor.seekTo` returned a signed integer index that encoded both position and whether the key was found (negative = not found, using the `(-1 - index)` sentinel convention). When iterating in descending order and the seek moved past the end of the tree, the method returned `-1` (not-found, index 0), but `-1 - (-1)` decoded to index `0`, pointing at the first element rather than indicating an out-of-bounds condition. `BTreeSearchIterator` then used this incorrect index to access an element it should not have visited. The fix refactors `seekTo` to return a plain boolean for the found/not-found signal, and has callers read the actual cursor position via `cur.globalIndex()`, eliminating the sentinel encoding entirely.

### Before / After

```java
// BEFORE (TreeCursor.java) — out-of-bounds returns ambiguous -1
{
    this.cur = root();
    return forwards ? -1 - size(rootNode()) : -1;
}
```

```java
// AFTER (TreeCursor.java) — out-of-bounds returns false
{
    this.cur = root();
    return false;
}
```

```java
// BEFORE (BTreeSearchIterator.java)
int index = seekTo(target, forwards, (state & (ON_ITEM | BEFORE_FIRST)) != 0);
boolean found = index >= 0;
if (!found) index = -1 - index;
```

```java
// AFTER (BTreeSearchIterator.java)
boolean found = seekTo(target, forwards, (state & (ON_ITEM | BEFORE_FIRST)) != 0);
int index = cur.globalIndex();
```

---

## How to Spot

Encoding two pieces of information (found/not-found and position) into a single signed integer is a pattern that is easily mishandled at edge cases, especially around index 0. Reviewers should be cautious of `(-1 - x)` sentinel conventions and verify all return paths — particularly the out-of-tree / out-of-bounds paths — produce a value that decodes correctly before accessing array elements.

---

## Test

The existing `LongBTreeTest` burn test was extended to generate a search key past the maximum element so that descending iteration past the end of the tree is exercised.
