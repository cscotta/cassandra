# Bug 1507: Fix potential AssertionError with 2ndary indexes

**Commit**: 3970c650d4 · **Ticket**: CASSANDRA-6612
**Subsystem**: src/java/org/apache/cassandra/db/index/
**Category**: Logic error in condition

---

## Root Cause

`SecondaryIndexManager.hasIndexFor()` was supposed to return `true` if any searcher covers the given clause, but its loop used the wrong short-circuit: it returned `false` on the first searcher that did *not* match and only returned `true` after exhausting all searchers without a failure. This is AND-logic instead of the required OR-logic, so a valid index was invisible whenever more than one searcher existed and not all of them matched the clause.

A second problem existed in `groupByIndexType`: the map keyed index entries by `getClass().getCanonicalName()`, which groups all subclasses of different internal index types under the same key if they happen to share a name, and was inconsistent across custom vs. built-in index implementations. The fix introduces `indexTypeForGrouping()` — overridden by `AbstractSimplePerColumnSecondaryIndex` to return the literal string `"_internal_"` — so all internal indexes land in one group while custom indexes continue to use their canonical class name.

### Before / After

```java
// BEFORE — AND-logic instead of OR-logic
for (SecondaryIndexSearcher searcher : searchers)
    if (!searcher.isIndexing(clause))
        return false;
return true;
```

```java
// AFTER — correct OR-logic
for (SecondaryIndexSearcher searcher : searchers)
    if (searcher.isIndexing(clause))
        return true;
return false;
```

---

## How to Spot

When iterating a collection to check "does any element satisfy condition X", the idiom is `return true` inside the loop and `return false` after. The negated form (`return false` inside, `return true` after) checks that *all* elements satisfy the condition. Double-check boolean short-circuit direction in any `hasX` / `isY` helper that iterates a list of candidates.

---

## Test

No test added.
