# Bug 0502: Force Compaction Silently Skips SSTables in Wrapping Token Ranges

**Commit**: 49abedc2c3 · **Ticket**: CASSANDRA-15664
**Subsystem**: `src/java/org/apache/cassandra/db/compaction/CompactionManager.java`
**Category**: Silent skip on non-match

---

## Root Cause

`CompactionManager#sstablesInBounds` called `View.sstablesInBounds(left.minKeyBound(), right.maxKeyBound(), tree)` directly on every token range in the compaction request. An interval tree lookup requires that `left <= right`; a wrapping range (one that crosses the ring's origin, where `left > right` numerically) violates this invariant, so the interval tree returns zero results. SSTables that held data in the wrapped portion of the range were silently omitted from the compaction, leaving them uncompacted even after an explicit `nodetool compact --partition-key`.

### Before / After

```java
// BEFORE — passes wrapping range directly to interval tree, which silently returns nothing
Iterable<SSTableReader> ssTableReaders =
    View.sstablesInBounds(tokenRange.left.minKeyBound(),
                          tokenRange.right.maxKeyBound(), tree);
Iterables.addAll(sstables, ssTableReaders);
```

```java
// AFTER — unwrap wrapping ranges before querying the interval tree
if (!AbstractBounds.strictlyWrapsAround(tokenRange.left, tokenRange.right)) {
    Iterables.addAll(sstables,
        View.sstablesInBounds(tokenRange.left.minKeyBound(),
                              tokenRange.right.maxKeyBound(), tree));
} else {
    for (Range<Token> unwrappedRange : tokenRange.unwrap()) {
        Iterables.addAll(sstables,
            View.sstablesInBounds(unwrappedRange.left.minKeyBound(),
                                  unwrappedRange.right.maxKeyBound(), tree));
    }
}
```

---

## How to Spot

Any code that feeds a `Range<Token>` directly into an interval-tree or sorted-boundary query without first checking `strictlyWrapsAround` (or calling `unwrap`) is potentially broken for partitioners with wrapping ranges (Murmur3 and Random). Reviewers should look for `sstablesInBounds`, `overlaps`, or `contains` calls that consume a range argument without a wrapping check, especially on user-facing paths like force-compaction or repair.

---

## Test

A test was added to `LeveledCompactionStrategyTest` that constructs a wrapping token range spanning both test keys and asserts that `forceCompactionForTokenRange` reduces 30 SSTables to 1.
