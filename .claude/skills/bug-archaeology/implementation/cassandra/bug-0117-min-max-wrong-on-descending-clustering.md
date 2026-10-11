# Bug 0117: min/max Aggregate Functions Return Wrong Results on Descending Clustering Columns

**Commit**: 881a1f023f · **Ticket**: CASSANDRA-20295
**Subsystem**: `src/java/org/apache/cassandra/cql3/functions/AbstractFunction.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`AbstractFunction` stored the `returnType` passed to its constructor without unwrapping it. For clustering columns declared with `CLUSTERING ORDER BY (col DESC)`, the type is a `ReversedType` wrapper around the real type. The `min` and `max` aggregate functions use `returnType` to compare candidate values; when `returnType` was a `ReversedType`, comparisons used reversed semantics, so `max` would return the smallest value in the column and `min` would return the largest. Calling `returnType.unwrap()` at construction time eliminates the wrapper, ensuring comparisons use the natural ordering regardless of clustering direction.

### Before / After

```java
// BEFORE – stores the type verbatim; may be a ReversedType for DESC columns
this.returnType = returnType;
```

```java
// AFTER – always works with the unwrapped base type
this.returnType = returnType != null ? returnType.unwrap() : null;
```

---

## How to Spot

Any code path that receives a column type and uses it for value comparison or aggregation must be aware that clustering column types may be wrapped in `ReversedType`. A single call to `type.unwrap()` (or `AbstractType.asCQL3Type()`) at the entry point prevents reversed-ordering artefacts from propagating through all downstream comparisons. During review, search for places that accept `AbstractType<?>` arguments and use them for `compareTo` or ordering without unwrapping first.

---

## Test

`AggregationTest#testMaxAggregationDescending` and `testMinAggregationDescending` insert rows into a table with `CLUSTERING ORDER BY (b DESC)` and assert that `max(b)` and `min(b)` return the numerically largest and smallest values respectively.
