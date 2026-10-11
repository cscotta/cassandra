# Bug 0164: NPE in ANN+WHERE Query When a Row Is Inserted in a Different Partition

**Commit**: 54cf65476a · **Ticket**: CASSANDRA-19404
**Subsystem**: src/java/org/apache/cassandra/index/sai/iterators/KeyRangeOrderingIterator.java
**Category**: Missing null / bounds check

---

## Root Cause

`KeyRangeOrderingIterator.performSkipTo` always called `nextIterator.skipTo(nextToken)` without checking whether `nextIterator` was null. The field is nullable: it is initialized lazily and can be null at the start of iteration when no "next" iterator has yet been computed. When an ANN+WHERE query involves multiple SAI indexes (e.g., a map-entry index and a vector index) and a row in a different partition is inserted after the initial query setup, the ordering iterator's `nextIterator` is null at the time `performSkipTo` is invoked, causing a NullPointerException.

### Before / After

```java
// BEFORE
protected void performSkipTo(PrimaryKey nextToken) {
    input.skipTo(nextToken);
    nextIterator.skipTo(nextToken);
}
```

```java
// AFTER
protected void performSkipTo(PrimaryKey nextToken) {
    input.skipTo(nextToken);
    if (nextIterator != null)
        nextIterator.skipTo(nextToken);
}
```

---

## How to Spot

Whenever a method unconditionally dereferences a field that is documented or typed as nullable, it is a latent NPE. For iterator classes that lazily initialize "look-ahead" or "next segment" fields, any method that operates on such a field must guard against the null case. During review, search for fields annotated `@Nullable` (or left as a plain reference initialized to `null`) and verify that every method that touches them includes a null check.

---

## Test

`VectorTypeTest.multiPartitionUpdateMultiIndexTest` creates a table with a vector index and a map-entry index, queries with ANN+WHERE at partition level, then inserts a row in a different partition and re-executes the query, asserting only the matching row is returned without a NPE.
