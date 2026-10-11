# Bug 1068: Index Selectivity Comparison Picks Least Selective Index

**Commit**: 2d472370b4 · **Ticket**: CASSANDRA-10435
**Subsystem**: src/java/org/apache/cassandra/index/SecondaryIndexManager.java
**Category**: Logic error in condition

---

## Root Cause

`SecondaryIndexManager.getBestIndexFor()` was supposed to choose the most selective secondary index (fewest estimated result rows) to drive a query. The stream terminal operation used `.max()` with `Longs.compare(a, b)`, which selects the index with the highest `getEstimatedResultRows()` value — the least selective index. The correct operation is `.min()`. Additionally, the candidate list was a `List`, allowing the same index to be added multiple times for multi-expression filters; switching to a `Set` de-duplicates entries.

### Before / After

```java
// BEFORE — .max() picks the index with the MOST estimated rows (least selective)
Index selected = searchableIndexes.size() == 1
                 ? searchableIndexes.get(0)
                 : searchableIndexes.stream()
                                    .max((a, b) -> Longs.compare(a.getEstimatedResultRows(),
                                                                 b.getEstimatedResultRows()))
                                    .orElseThrow(...);
```

```java
// AFTER — .min() picks the index with the FEWEST estimated rows (most selective)
Index selected = searchableIndexes.size() == 1
                 ? Iterables.getOnlyElement(searchableIndexes)
                 : searchableIndexes.stream()
                                    .min((a, b) -> Longs.compare(a.getEstimatedResultRows(),
                                                                 b.getEstimatedResultRows()))
                                    .orElseThrow(...);
```

---

## How to Spot

Whenever a `min`/`max` stream operation is used to select the "best" item from a collection, verify the intended ordering. The mnemonic: "most selective" means fewest rows, which is the minimum. A code review should confirm that the comparator's ordering and the terminal operation (`min` vs `max`) together produce the intended winner. Bugs of this type are invisible without a test that checks actual index selection behavior with two indexes of differing estimated cardinality.

---

## Test

`CustomIndexTest.indexSelectionPrefersMostSelectiveIndex()` added. It creates two custom indexes with controllable selectivity estimates and asserts that the index with the lower estimated result count is chosen to execute the query.
