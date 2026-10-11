# Bug 2498: CQL counter decrement with double-negative (field = field - -N) applies wrong sign

**Commit**: 68e224b514 · **Ticket**: CASSANDRA-3418

## Detection Rule

When a grammar rule for `field = field - <expr>` handles the optional unary minus by negating the parsed value in the expression execution layer rather than at grammar parse time, a double-negative literal (`- -5`) will have its sign flipped twice, causing incorrect counter arithmetic.

## Code Smell

`if (op.type == OperationType.MINUS) { if (value > 0) value *= -1; }` in the execution layer: this unconditionally negates any positive value for MINUS operations, but if the grammar already stripped the negative sign from a negative literal like `-5`, the value arrives as `5` (positive) and gets negated to `-5`, then MINUS applies, resulting in `+5` instead of the expected `+5`. However when the literal is `- -5`, the parser produces `5`, execution negates to `-5`, and MINUS adds negative `-5` — wrong direction.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: double-negation-sign-handling
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause

The CQL grammar's `termPairWithOperation` rule captured an optional `-` operator token but did not negate the value in the grammar action. Instead, `UpdateStatement` tried to conditionally negate the value at execution time. For the expression `field = field - -5`, the grammar produced a positive term `5`, and the execution code negated positive values to `-5` for MINUS operations — effectively computing `field + (-5)` instead of `field - (-5) = field + 5`. The fix moves the negation into the grammar action (`v = new Term(-Long.valueOf(v.getText()), v.getType())` when `op != null`) and removes the execution-layer sign flip.

## Example

```java
// BEFORE (grammar)
op='-'? v=intTerm
    { validateMinusSupplied(op, v, input);
      columns.put(key, new Operation(c, OperationType.MINUS, v)); }
// BEFORE (execution)
if (op.type == OperationType.MINUS)
    if (value > 0) value *= -1;
```

```java
// AFTER (grammar)
op='-'? v=intTerm
    { validateMinusSupplied(op, v, input);
      if (op != null) v = new Term(-(Long.valueOf(v.getText())), v.getType());
      columns.put(key, new Operation(c, OperationType.MINUS, v)); }
// AFTER (execution): sign-flip code removed entirely
```
