# Bug 1627: Fix IllegalStateException in CqlPagingRecordReader for tables with few rows

**Commit**: 453a07430c · **Ticket**: CASSANDRA-7198
**Subsystem**: hadoop/cql3 (CqlPagingRecordReader)
**Category**: Missing null / bounds check

---

## Root Cause

`CqlPagingRecordReader.initialize` called `CqlConfigHelper.getInputPageRowSize(conf).get()` directly. When the configuration key was absent the method returned an empty `Optional`, and calling `.get()` on an empty `Optional` throws `IllegalStateException` (not `NumberFormatException`). The surrounding `catch (NumberFormatException)` block therefore never fired, so the reader crashed instead of falling back to the default page limit. The fix checks `isPresent()` before calling `.get()`.

### Before / After

```java
// BEFORE
try
{
    pageRowSize = CqlConfigHelper.getInputPageRowSize(conf).get();
}
catch (NumberFormatException e)
{
    pageRowSize = DEFAULT_CQL_PAGE_LIMIT;
}
```

```java
// AFTER
Optional<Integer> pageRowSizeOptional = CqlConfigHelper.getInputPageRowSize(conf);
try
{
    pageRowSize = pageRowSizeOptional.isPresent() ? pageRowSizeOptional.get() : DEFAULT_CQL_PAGE_LIMIT;
}
catch (NumberFormatException e)
{
    pageRowSize = DEFAULT_CQL_PAGE_LIMIT;
}
```

---

## How to Spot

Calling `.get()` on a Guava (or Java) `Optional` without first calling `.isPresent()` always risks an exception at runtime. During code review, flag every `.get()` on an `Optional` that lacks a preceding `.isPresent()` check or `.orElse()` fallback.

---

## Test

No test added.
