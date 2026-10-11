# Bug 2135: Wrong Component Index Calculation When Adding Collection Column via ALTER TABLE

**Commit**: 8349fcebfb · **Ticket**: CASSANDRA-5117
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/AlterTableStatement.java
**Category**: Off-by-one

---

## Root Cause

`AlterTableStatement` computed `componentIndex` eagerly before determining whether the new column was a collection type, then conditionally decremented it twice (`componentIndex--`) in separate branches — once for the collection type path, and once for the "table already has collections" path. This sequential decrement logic was fragile and produced an incorrect (too-small) component index when adding a collection column to a table that already contained collections, causing an exception. The fix recalculates `componentIndex` at the end with a single correct expression that accounts for the presence or absence of existing collections in one step.

### Before / After

```java
// BEFORE — eager init plus two conditional decrements, wrong when both conditions hold
Integer componentIndex = cfDef.isComposite ? ((CompositeType)meta.comparator).types.size() - 1 : null;
// ... collection branch: componentIndex--;
// ... else-if hasCollections branch: componentIndex--;
```

```java
// AFTER — single expression computed after the comparator is finalized
Integer componentIndex = cfDef.isComposite
    ? ((CompositeType)meta.comparator).types.size() - (cfDef.hasCollections ? 2 : 1)
    : null;
```

---

## How to Spot

When an index or offset is computed by starting from a base value and applying conditional decrements across multiple branches, it is easy to apply the wrong number of decrements. Prefer computing the final value in a single expression that encodes all conditions explicitly, rather than mutating a variable through a sequence of `if/else` blocks.

---

## Test

No test added in this commit.
