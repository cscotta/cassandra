# Bug 1850: Fix Missing One Row During Reverse Query on Compact Tables

**Commit**: 3cb5854e92 · **Ticket**: CASSANDRA-6330
**Subsystem**: cql3/statements/SelectStatement
**Category**: Off-by-one

---

## Root Cause

`SelectStatement.getLimit()` inflated the internal fetch limit by 1 only when the slice had an exclusive start bound. For compact storage tables, both the start and end bounds of a slice can be exclusive, so a reverse query (which effectively makes the end bound the start of iteration) could use an exclusive end without an exclusive start. In that case the extra row was never fetched, meaning the caller received one fewer row than the requested limit after the post-fetch exclusion step.

### Before / After

```java
// BEFORE
return sliceRestriction != null
       && !sliceRestriction.isInclusive(Bound.START)
       && parameters.limit != Integer.MAX_VALUE
     ? parameters.limit + 1
     : parameters.limit;
```

```java
// AFTER
return (sliceRestriction != null
        && parameters.limit != Integer.MAX_VALUE
        && (!sliceRestriction.isInclusive(Bound.START) || !sliceRestriction.isInclusive(Bound.END)))
     ? parameters.limit + 1
     : parameters.limit;
```

---

## How to Spot

Whenever code adds padding to a limit to compensate for post-query filtering, verify that every code path that performs filtering is matched by a corresponding +1. Here, checking only the start bound missed the symmetric end-bound case that arises in reverse scans.

---

## Test

No test added.
