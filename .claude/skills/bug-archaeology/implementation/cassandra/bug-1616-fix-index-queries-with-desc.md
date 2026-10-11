# Bug 1616: Fix 2ndary Index Queries with DESC Clustering Order

**Commit**: 20e058b2bd · **Ticket**: CASSANDRA-6950
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When building secondary index `IndexExpression` objects for range predicates, the code passed the operator directly from the user's query without accounting for reversed clustering columns. A column declared with `DESC` clustering order uses a `ReversedType` comparator internally, which flips the physical byte ordering. The secondary index machinery uses the underlying comparator as-is, so a user-written `col > ?` on a DESC column must be translated to a physical `GT -> LT` (and vice versa) before being handed to the index. Without this flip, range queries against a secondary index on a descending clustering column return wrong or empty results.

### Before / After

```java
// BEFORE
expressions.add(new IndexExpression(name.name.key, slice.getIndexOperator(b), value));
```

```java
// AFTER
IndexOperator op = slice.getIndexOperator(b);
if (name.type instanceof ReversedType)
    op = reverse(op);
expressions.add(new IndexExpression(name.name.key, op, value));

private static IndexOperator reverse(IndexOperator op)
{
    switch (op)
    {
        case LT:  return IndexOperator.GT;
        case LTE: return IndexOperator.GTE;
        case GT:  return IndexOperator.LT;
        case GTE: return IndexOperator.LTE;
        default: return op;
    }
}
```

---

## How to Spot

When translating user-facing query operators into internal storage operators, look for any step where the column type is used for comparison but the code ignores whether the type is a `ReversedType` wrapper. Any operator translation that operates on raw `IndexOperator` values without checking for reversed ordering is suspect.

---

## Test

No test added.
