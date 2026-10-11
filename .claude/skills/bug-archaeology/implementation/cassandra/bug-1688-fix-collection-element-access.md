# Bug 1688: Fix Collection Element Access in IF

**Commit**: 5aafa98768 · **Ticket**: CASSANDRA-6914
**Subsystem**: src/java/org/apache/cassandra/cql3/ColumnCondition.java
**Category**: Logic error in condition

---

## Root Cause

`IF` conditions in conditional statements (`UPDATE ... IF col = value`) had no support for element-level access on collections such as `IF map[key] = value` or `IF list[idx] = value`. The grammar only parsed simple equality, so any attempt to test a specific element of a map or list was silently unsupported. The fix extended both the ANTLR grammar and `ColumnCondition` to carry an optional `collectionElement` term alongside the expected value, and added `mapElementAppliesTo` and `listElementAppliesTo` evaluation helpers that do a direct cell lookup (for maps) or an iterator advance (for lists).

### Before / After

```java
// BEFORE
columnCondition[List<Pair<ColumnIdentifier, ColumnCondition.Raw>> conditions]
    : key=cident '=' t=term { conditions.add(Pair.create(key, new ColumnCondition.Raw(t))); }
    ;
```

```java
// AFTER
columnCondition[List<Pair<ColumnIdentifier, ColumnCondition.Raw>> conditions]
    : key=cident '=' t=term { conditions.add(Pair.create(key, ColumnCondition.Raw.simpleEqual(t))); }
    | key=cident '[' element=term ']' '=' t=term { conditions.add(Pair.create(key, ColumnCondition.Raw.collectionEqual(t, element))); }
    ;
```

---

## How to Spot

When a CQL feature refers to sub-element access (e.g., `col[key]`), check that every conditional path—grammar, AST node, evaluation logic, and duplicate-detection map key—all carry the element selector. A map keyed only by column name will incorrectly merge conditions on different elements of the same collection.

---

## Test

No dedicated unit test added in this commit; the grammar rule addition and the map/list element evaluation helpers constitute the primary verification.
