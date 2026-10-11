# Bug 2083: AssertionError During Repair from Partitioner Type Check and MerkleTree Depth Overflow

**Commit**: e4b3d403f3 · **Ticket**: CASSANDRA-5245
**Subsystem**: src/java/org/apache/cassandra/service/AntiEntropyService.java, src/java/org/apache/cassandra/utils/MerkleTree.java
**Category**: Logic error in condition

---

## Root Cause

Two related problems triggered an `AssertionError` during anti-entropy repair:

1. `AntiEntropyService.prepare()` called `tree.init()` (which assumes a hash-based partitioner with no natural order) only when the partitioner was `instanceof RandomPartitioner`. This left out other non-order-preserving partitioners, so a tree built for them was not properly initialized, leading to assertion failures during comparison.

2. `MerkleTree.differenceHelper()` recursed by splitting ranges in half to compute tree depth. When tokens were extremely close together, the computed midpoint could equal one of the endpoints, causing the recursion to make no progress and eventually overflow the `byte` depth counter, triggering an `AssertionError` at `Byte.MAX_VALUE + 1`.

### Before / After

```java
// BEFORE — AntiEntropyService.java
if (tree.partitioner() instanceof RandomPartitioner)
{
    tree.init();
```

```java
// AFTER — AntiEntropyService.java
if (!tree.partitioner().preservesOrder())
{
    tree.init();
```

```java
// AFTER — MerkleTree.java (guard added at top of differenceHelper)
if (active.depth == Byte.MAX_VALUE)
    return CONSISTENT;
```

---

## How to Spot

Partitioner type checks using `instanceof` against a concrete class are fragile — they break whenever a new partitioner is added. Prefer capability methods like `preservesOrder()` that express the property of interest. Additionally, any recursive algorithm that increments a bounded counter (`byte`, `short`) without a corresponding depth-limit guard risks overflow.

---

## Test

No test added.
