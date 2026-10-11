# Bug 1399: Multi-Column Relations Crash or Produce Wrong Results on COMPACT STORAGE Tables

**Commit**: 084d93daf6 · **Ticket**: CASSANDRA-8264
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Logic error in condition

---

## Root Cause

In `SelectStatement.getRequestedColumns()`, the code distinguished between EQ and IN restrictions solely by counting the number of values returned by `r.values(variables)`: one value meant EQ, more than one meant IN. This heuristic broke for multi-column restrictions, which return a flat list of per-component values rather than per-row values, so the size check was meaningless. On COMPACT STORAGE tables the wrong branch was taken, causing either an `IndexOutOfBoundsException` or silently incorrect column name construction. The fix changes the dispatch to use the semantic predicate `r.isEQ()` for the equality branch and then separately checks `r.isMultiColumn()` inside the non-EQ branch to invoke the correct multi-column IN handling path.

### Before / After

```java
// BEFORE: dispatch by value list size — wrong for multi-column restrictions
List<ByteBuffer> values = r.values(variables);
if (values.size() == 1)
{
    // EQ path
}
else
{
    // IN path — used r.values() which is wrong for multi-column IN
}
```

```java
// AFTER: dispatch by restriction type
if (r.isEQ())
{
    List<ByteBuffer> values = r.values(variables);
    // EQ path
}
else
{
    if (!r.isMultiColumn())
    {
        // single-column IN path
    }
    else
    {
        // multi-column IN path using splitValues()
    }
}
```

---

## How to Spot

Any code that inspects the result of `restriction.values()` to infer the *type* of restriction (EQ vs IN) rather than calling a typed predicate (`isEQ()`, `isIN()`, `isMultiColumn()`) is fragile. During a review, check every branch that special-cases `values.size() == 1` against a restriction object: the correct question is always "what kind of restriction is this?" not "how many values does it happen to return right now?"

---

## Test

`MultiColumnRelationTest` was extended to run all existing multi-column test cases against both regular and `COMPACT STORAGE` table variants, covering equality, IN, and slice restrictions.
