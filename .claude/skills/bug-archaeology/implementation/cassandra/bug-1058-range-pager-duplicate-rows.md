# Bug 1058: RangeNamesQueryPager Returns Duplicate Rows at Page Boundaries

**Commit**: 289b7b7cef · **Ticket**: CASSANDRA-10509
**Subsystem**: Query Paging / RangeNamesQueryPager
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`RangeNamesQueryPager.containsPreviousLast()` always returned `false`, signalling to the base class that the first row of a new page never duplicates the last row from the previous page. To exclude the last-returned key from the next page range, `nextPageReadCommand()` constructs an `ExcludingBounds`. However, `ExcludingBounds` is serialized over the wire as a plain `Bounds`, which includes both endpoints. Consequently, the node serving the next page request still returns the row that was the last row of the previous page. Because `containsPreviousLast()` unconditionally returned `false`, the base pager never stripped that duplicate, so callers received the same row twice at every page boundary.

### Before / After
```java
// BEFORE
protected boolean containsPreviousLast(Row first) {
    return false;
}

// AFTER
protected boolean containsPreviousLast(Row first) {
    // ExcludingBounds is serialized as Bounds, so the boundary key may
    // still appear as the first row of the next page.
    return lastReturnedKey != null && lastReturnedKey.equals(first.key);
}
```

---

## How to Spot
Any pager that constructs an `ExcludingBounds` for range advancement must account for the serialization-as-`Bounds` issue. If `containsPreviousLast()` always returns `false` in a subclass that uses `ExcludingBounds`, duplicate rows will appear at every page boundary when the pager is used across nodes that communicate with the serialized form.

---

## Test
No test added.
