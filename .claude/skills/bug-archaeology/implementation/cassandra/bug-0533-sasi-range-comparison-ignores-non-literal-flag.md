# Bug 0533: SASI Range Comparison Ignores Non-Literal String Flag

**Commit**: b20daee68d · **Ticket**: CASSANDRA-15169
**Subsystem**: `src/java/org/apache/cassandra/index/sasi/plan/Expression.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`Expression.satisfiesLowerBound()` and `satisfiesUpperBound()` always passed `false` as the `isSuffix` parameter to `term.compareTo()`. For non-literal (range) string expressions — such as `LIKE` prefix or suffix matches — the comparison must use a different comparison mode that accounts for the variable-length nature of the indexed token. By hardcoding `false`, range operators on non-literal SASI string columns produced incorrect boundary evaluations, causing rows that should match a range query to be excluded or rows outside the range to be included.

### Before / After

```java
// BEFORE: always passes false regardless of expression type
int cmp = term.compareTo(validator, lower.value, false);
// ...
int cmp = term.compareTo(validator, upper.value, false);
```

```java
// AFTER: passes true for non-literal RANGE expressions
int cmp = term.compareTo(validator, lower.value, operation == Op.RANGE && !isLiteral);
// ...
int cmp = term.compareTo(validator, upper.value, operation == Op.RANGE && !isLiteral);
```

---

## How to Spot

In index expression evaluators, look for places where a comparison helper accepts a mode flag but that flag is hardcoded rather than derived from the expression's type. When the same code path handles both literal equality and non-literal range matches, any hardcoded mode argument should immediately raise the question: "Is this the correct mode for all callers, or only for one sub-type?"

---

## Test

A comprehensive SASI CQL test suite adds cases for `<`, `>`, `<=`, `>=` range operators on string columns and asserts the correct rows are returned.
