# Bug 1698: Fix potential NPE in ColumnFamilyStore#filterColumnFamily

**Commit**: 5749fb1eaa · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/ColumnFamilyStore.java
**Category**: Missing null / bounds check

---

## Root Cause

`ColumnFamilyStore.filterColumnFamily()` assumed its `cached` parameter was never `null`, immediately calling `cached.cloneMeShallow(...)` on the first line. When a cache lookup misses and returns `null`, the method would throw a `NullPointerException`. The fix adds an explicit null guard at the top of the method and returns `null` immediately, letting callers handle the cache-miss case they already expected to receive.

### Before / After

```java
// BEFORE
ColumnFamily filterColumnFamily(ColumnFamily cached, QueryFilter filter)
{
    ColumnFamily cf = cached.cloneMeShallow(...);
```

```java
// AFTER
ColumnFamily filterColumnFamily(ColumnFamily cached, QueryFilter filter)
{
    if (cached == null)
        return null;

    ColumnFamily cf = cached.cloneMeShallow(...);
```

---

## How to Spot

Any private helper that accepts an object parameter and immediately dereferences it without a null check is a candidate for NPE if calling code can ever produce a null (e.g., cache misses, optional returns). Review call sites to see whether `null` is a documented possible value.

---

## Test

No test added.
