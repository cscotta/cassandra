# Bug 0188: SAI Unindexed Context Always Uses VALUES Target Type, Ignores CONTAINS KEY

**Commit**: c26f2a3d69 · **Ticket**: CASSANDRA-19040
**Subsystem**: index/sai/plan
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

In `QueryController.getIndexContext`, when no index exists for a column expression, a synthetic `IndexContext` is created for post-filtering. The code hardcoded `IndexTarget.Type.VALUES` for this synthetic context regardless of the operator used in the query. When the query contained a `CONTAINS KEY` operator, the context should use `IndexTarget.Type.KEYS`, but the wrong type was used, causing the unindexed predicate to be treated as a value check instead of a key check and producing wrong query results.

### Before / After

```java
// BEFORE
IndexTarget.Type.VALUES,
```

```java
// AFTER
expression.operator().isContainsKey()
        ? IndexTarget.Type.KEYS
        : IndexTarget.Type.VALUES,
```

---

## How to Spot

When constructing a fallback or synthetic index descriptor from a query expression, all attributes of that descriptor must reflect the actual expression semantics, not a hardcoded default. Any literal constant in an expression-builder path (such as `Type.VALUES`) should be verified against all possible operator variants that could flow through that path.

---

## Test

`unindexedContainsExpressions` creates a table with an SAI index only on a non-map column, then queries with `CONTAINS KEY` on the unindexed map column (with `ALLOW FILTERING`) and asserts the correct single row is returned.
