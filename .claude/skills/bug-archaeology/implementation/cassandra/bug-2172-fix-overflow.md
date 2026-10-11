# Bug 2172: Integer Overflow in SelectStatement.getLimit() With Max Value

**Commit**: e39bf7a3df · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Off-by-one

---

## Root Cause

`SelectStatement.getLimit()` added 1 to the limit when a slice restriction had an exclusive start bound, to compensate for the extra element that would be fetched and then discarded. When no explicit `LIMIT` was specified, the default was `Integer.MAX_VALUE`. Adding 1 to `Integer.MAX_VALUE` wraps to `Integer.MIN_VALUE` (negative), causing downstream range-slice logic to request a negative number of results and producing incorrect or empty query results.

### Before / After

```java
// BEFORE — overflows when limit == Integer.MAX_VALUE
return sliceRestriction != null && !sliceRestriction.isInclusive(Bound.START)
     ? parameters.limit + 1
     : parameters.limit;
```

```java
// AFTER — guards against overflow
return sliceRestriction != null && !sliceRestriction.isInclusive(Bound.START) && parameters.limit != Integer.MAX_VALUE
     ? parameters.limit + 1
     : parameters.limit;
```

---

## How to Spot

Any arithmetic that adds a constant to a value that may be `Integer.MAX_VALUE` (commonly used as a sentinel for "unlimited") must guard against overflow. Review all `limit + 1` or `count + 1` patterns to confirm the maximum sentinel is excluded from the increment path.

---

## Test

No test added.
