# Bug 1602: Fix Bound Building for Multi-Column IN Restrictions

**Commit**: 6574de5342 · **Ticket**: CASSANDRA-6875 (follow-up)
**Subsystem**: src/java/org/apache/cassandra/cql3/statements
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`buildMultiColumnInBound()` accepted a `Collection<CFDefinition.Name>` and consumed it via an `Iterator` to decide per-component whether the bound should be reversed. The iterator advanced once per component across all IN-value tuples, meaning the reversal decision was tied to iterator position rather than to whether the table's clustering order was reversed. The fix removes the per-column iterator logic, computes a single `eocBound` from `isReversed` once before the loop, and applies it consistently to every component — matching how single-column IN bounds already worked.

### Before / After

```java
// BEFORE
private List<ByteBuffer> buildMultiColumnInBound(Bound bound,
                                                 Collection<CFDefinition.Name> names,
                                                 MultiColumnRestriction.IN restriction,
                                                 boolean isReversed,
                                                 ColumnNameBuilder builder,
                                                 List<ByteBuffer> variables) throws InvalidRequestException
{
    List<List<ByteBuffer>> splitInValues = restriction.splitValues(variables);
    ...
    Iterator<CFDefinition.Name> iter = names.iterator();
    for (List<ByteBuffer> components : splitInValues)
    {
        ...
        Bound b = isReversed == isReversedType(iter.next()) ? bound : Bound.reverse(bound);
        inValues.add((bound == Bound.END && nameBuilder.remainingCount() > 0) ? nameBuilder.buildAsEndOfRange() : nameBuilder.build());
    }
```

```java
// AFTER
private List<ByteBuffer> buildMultiColumnInBound(Bound bound,
                                                 MultiColumnRestriction.IN restriction,
                                                 boolean isReversed,
                                                 ColumnNameBuilder builder,
                                                 List<ByteBuffer> variables) throws InvalidRequestException
{
    List<List<ByteBuffer>> splitInValues = restriction.splitValues(variables);
    Bound eocBound = isReversed ? Bound.reverse(bound) : bound;
    ...
    for (List<ByteBuffer> components : splitInValues)
    {
        ...
        inValues.add((eocBound == Bound.END && nameBuilder.remainingCount() > 0) ? nameBuilder.buildAsEndOfRange() : nameBuilder.build());
    }
```

---

## How to Spot

When an iterator is consumed inside a loop that iterates over a different collection, check that the two collections are always in lock-step. A stateful iterator whose position depends on iteration count of a different collection is a strong sign the logic will diverge for any non-trivial input.

---

## Test

New `testLiteralInReversed()` test in `MultiColumnRelationTest` — inserts rows with reversed clustering columns, then executes `IN` queries with both normal and reversed IN-value ordering and asserts correct row counts and order.
