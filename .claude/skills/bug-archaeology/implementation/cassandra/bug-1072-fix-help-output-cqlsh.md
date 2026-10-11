# Bug 1072: Fix HELP SELECT_EXPR Output in cqlsh

**Commit**: 2e87c433a9 · **Ticket**: CASSANDRA-10074
**Subsystem**: pylib/cqlshlib/helptopics.py
**Category**: Wrong constant / default value

---

## Root Cause

The cqlsh HELP text for `SELECT_EXPR` was written when `COUNT` was the only aggregate function supported. When `MIN`, `MAX`, `SUM`, and `AVG` were subsequently added to CQL, the help text was never updated to document them. The documentation claimed "COUNT is the only function supported by CQL," leaving users unaware of four valid aggregate functions.

### Before / After

```python
# BEFORE
        single row will be returned, with a single column named "count" whose
        value is the number of rows from the pre-aggregation resultset.

        Currently, COUNT is the only function supported by CQL.
```

```python
# AFTER
          SELECT MIN(name1), MAX(name2), SUM(name3), AVG(name4) FROM ...

        ...

        The MAX and MIN functions can be used to compute the maximum and the
        minimum value returned by a query for a given column.

        The SUM function can be used to sum up all the values returned by
        a query for a given column.

        The AVG function can be used to compute the average of all the
        values returned by a query for a given column.
```

---

## How to Spot

When new features are added to a query language, every place that enumerates "supported" operations becomes a potential staleness hazard. Reviewers should search for phrases like "only function supported," "supported operations are," or similar exhaustive lists whenever a feature-adding patch lands, and verify the documentation was updated alongside the implementation.

---

## Test

No test added.
