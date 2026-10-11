# Bug 2335: CQL3 range query with secondary index NPEs on one-sided range restriction

**Commit**: cbf0436181 · **Ticket**: CASSANDRA-4257

## Detection Rule

When iterating over `Bound.values()` (START, END) to build index expressions for a range restriction, check whether the restriction actually has a bound value for each side before dereferencing it.

## Code Smell

Unconditionally calling `restriction.bound(b).getByteBuffer(...)` in a loop over both bounds, where a half-open range restriction (e.g., `col > x` with no upper bound) returns `null` for the missing side.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: unconditional-dereference-in-bound-iteration
- severity: crash
- subsystem: sai-secondary-index

## Root Cause

`SelectStatement.getIndexExpressions()` iterated over `Bound.values()` to construct index filter expressions for each restricted column. For a half-open range restriction (e.g., `x > 5` with no upper bound), `restriction.bound(Bound.END)` returned `null`. The code then called `.getByteBuffer(...)` on it immediately, causing an NPE. The fix adds `if (restriction.bound(b) != null)` before dereferencing.

## Example

```java
// BEFORE
for (Bound b : Bound.values())
{
    ByteBuffer value = restriction.bound(b).getByteBuffer(name.type, variables);  // NPE if bound is null
    expressions.add(new IndexExpression(name.name.key, restriction.getIndexOperator(b), value));
}
```

```java
// AFTER
for (Bound b : Bound.values())
{
    if (restriction.bound(b) != null)
    {
        ByteBuffer value = restriction.bound(b).getByteBuffer(name.type, variables);
        expressions.add(new IndexExpression(name.name.key, restriction.getIndexOperator(b), value));
    }
}
```
