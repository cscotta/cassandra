# Bug 1849: Fix Missing Results on Query with IN Clauses

**Commit**: acea7160ed · **Ticket**: CASSANDRA-6327
**Subsystem**: db/marshal/CompositeType
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`CompositeType.intersects()` was supposed to return `true` when any of the IN-clause column ranges overlaps the sstable min/max column name bounds. The logic was inverted: it returned `false` as soon as any single component intersected (i.e. it acted as a logical AND of "does not intersect" predicates) and fell through to `return true` only when none did. The effect was that sstables were incorrectly skipped during reads, silently dropping rows that matched an IN predicate.

### Before / After

```java
// BEFORE
if (!t.intersects(minColumnNames.get(i), maxColumnNames.get(i), s, f))
    return false;
// ...
return true;
```

```java
// AFTER
if (t.intersects(minColumnNames.get(i), maxColumnNames.get(i), s, f))
    return true;
// ...
return false;
```

---

## How to Spot

When writing a short-circuit loop that checks "does any element satisfy condition X", the early-return should fire on a positive match (`if (X) return true`) and the default after the loop should be `return false`. Swapping the two silently flips the semantics from OR to NOT-AND and will pass casual review because the structure looks plausible.

---

## Test

No test added.
