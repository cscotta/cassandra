# Bug 1345: Fix reversal of list prepends

**Commit**: 2d8bddb064 · **Ticket**: CASSANDRA-8733
**Subsystem**: src/java/org/apache/cassandra/cql3/Lists.java
**Category**: Off-by-one

---

## Root Cause

When prepending multiple elements to a CQL list, the loop that assigned UUID-based time-ordered cell names iterated in forward order (`0` to `size-1`) while reusing (rather than copying) the `ColumnNameBuilder` only on the last iteration. The intent was to assign monotonically decreasing timestamps so that the prepended elements appear before existing ones, but because the builder was recycled at the wrong boundary the resulting element order was reversed relative to what the user supplied. The fix iterates in reverse (`size-1` down to `0`) and reuses the builder only on the final iteration (index `0`), producing the correct relative ordering.

### Before / After

```java
// BEFORE
for (int i = 0; i < toAdd.size(); i++)
{
    ColumnNameBuilder b = i == toAdd.size() - 1 ? column : column.copy();
    // ...
}
```

```java
// AFTER
for (int i = toAdd.size() - 1; i >= 0; i--)
{
    ColumnNameBuilder b = i == 0 ? column : column.copy();
    // ...
}
```

---

## How to Spot

Any loop that assigns time-based or sequence-based keys to an ordered collection must iterate in the order that matches the desired final sort order of those keys. When prepending, elements should be written in reverse so that the first element in the input list receives the latest (highest-priority) timestamp and sorts first. A reviewer should check whether the loop direction and the builder-reuse sentinel are consistent with each other and with the intended ordering semantics.

---

## Test

No test added.
