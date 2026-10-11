# Bug 0958: SASI EQ on analyzed index treated as full-value equality

**Commit**: 479e8aff13 · **Ticket**: CASSANDRA-11130
**Subsystem**: Secondary Indexes / SASI
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
SASI supports both analyzed (tokenizing) indexes and non-analyzed ones. A `LIKE '<term>'` query — meaning "exactly this token" in the context of a tokenizing index — was being parsed in the CQL grammar as `Operator.EQ`, the same operator used for a non-analyzed exact-match query. Because `EQ` on an analyzed index is supposed to match the full stored string (e.g., `WHERE v = 'Jean-Claude'` on a PREFIX index), routing `LIKE '<term>'` through `EQ` caused the analyzed-index code path to behave incorrectly: the query was not tokenized, and individual tokens such as `'Jean'` or `'Claude'` did not match `'Jean-Claude'`.

The fix introduces a new `Operator.LIKE_MATCHES` (id 13) and a corresponding `Expression.Op.MATCH`, separated from `EQ`. The CQL grammar now emits `LIKE_MATCHES` for the plain `LIKE '<term>'` form. `ColumnIndex.supports()` rejects `EQ` for tokenized indexes and rejects `RANGE` for literal indexes. The storage layer, TrieMemIndex, and query planner all gained branches for `LIKE_MATCHES`/`MATCH`, ensuring the term is tokenized before lookup on analyzed indexes.

### Before / After
```java
// BEFORE — CQL grammar (Cql.g)
// LIKE '<term>' (no wildcard) parsed as:
operator = Operator.EQ;

// AFTER
operator = Operator.LIKE_MATCHES;

// BEFORE — ColumnIndex.supports()
public boolean supports(Operator operator) {
    return mode.supports(Expression.Op.valueOf(operator));
}

// AFTER
public boolean supports(Operator op) {
    Op operator = Op.valueOf(op);
    return !(isTokenized && operator == Op.EQ)   // EQ not valid on tokenized indexes
        && !(isLiteral() && operator == Op.RANGE) // RANGE not valid on literal indexes
        && mode.supports(operator);
}
```

---

## How to Spot
When a secondary-index implementation distinguishes between analyzed and non-analyzed modes, the operator set that makes sense for each mode is different. Any `supports(Operator)` gate that delegates entirely to the on-disk mode without first checking whether tokenization changes the semantics of `EQ` can silently route the wrong operator through. During review, trace the path from the CQL grammar token (e.g., `LIKE '<term>'`) through `Operator`, `Expression.Op`, and finally to the index-lookup method, confirming that the operator variant reaching the index accurately represents the query intent for both tokenized and non-tokenized modes.

---

## Test
`SASIIndexTest.testLIKEAndEQSemanticsWithDifferenceKindsOfIndexes()` — exercises CONTAINS, PREFIX, and analyzed-PREFIX indexes with `LIKE`, `=`, `LIKE '%...'`, `LIKE '...%'`, and `LIKE '...'` patterns, verifying that tokenized indexes accept individual token matches via `LIKE '<term>'` but reject bare `=` for partial tokens, and that non-tokenized indexes do the opposite.
