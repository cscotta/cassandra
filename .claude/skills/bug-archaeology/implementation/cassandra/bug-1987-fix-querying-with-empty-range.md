# Bug 1987: Querying With an Empty (Impossible) Token Range Returns Full Ring Instead of Empty Result

**Commit**: 0beab74df9 · **Ticket**: CASSANDRA-5573
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java, src/java/org/apache/cassandra/db/filter/ColumnSlice.java, src/java/org/apache/cassandra/cql3/QueryProcessor.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When a CQL SELECT specified a token range where `startToken > endToken` (or equal tokens with exclusive bounds), the code passed the inverted range directly to `StorageProxy.getRangeSlice()`. The storage layer interpreted this as a wrap-around range and returned results from the entire ring instead of an empty set. CQL semantics do not support wrap-around ranges, so an impossible range should produce zero rows.

The fix introduces an explicit emptiness check at the range-building stage: `getKeyBounds()` returns `null` for logically empty ranges, and callers short-circuit to an empty result list without ever reaching the storage layer. A parallel fix adds `ColumnSlice.isAlwaysEmpty()` to detect impossible column slice orderings (start > finish in traversal direction) and return `null` from `makeFilter()` similarly.

### Before / After

```java
// BEFORE: impossible token range forwarded to storage, returns full ring
Token startToken = getTokenBound(Bound.START, variables, p);
Token endToken = getTokenBound(Bound.END, variables, p);
RowPosition start = includeStart ? startToken.minKeyBound() : startToken.maxKeyBound();
RowPosition end   = includeEnd   ? endToken.maxKeyBound()  : endToken.minKeyBound();
bounds = new Range<RowPosition>(start, end); // wraps around if start > end
```

```java
// AFTER: detect impossible range and return null early
int cmp = startToken.compareTo(endToken);
if (!startToken.isMinimum() && !endToken.isMinimum()
        && (cmp > 0 || (cmp == 0 && (!includeStart || !includeEnd))))
    return null; // caller returns emptyList()
```

---

## How to Spot

Range query handling that converts user-supplied bounds into a ring `Range` without first checking whether the resulting range is logically empty is a candidate for this bug class. Pay special attention to token comparisons where the ring's wrap-around semantics differ from numeric ordering semantics expected by callers.

---

## Test

No new test added in this commit; existing CQL query tests cover the range query path.
