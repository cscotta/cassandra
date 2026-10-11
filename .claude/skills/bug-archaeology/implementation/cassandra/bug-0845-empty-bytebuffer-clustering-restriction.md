# Bug 0845: Empty ByteBuffer in clustering restriction crashes or returns wrong rows

**Commit**: 527d1897c0 · **Ticket**: CASSANDRA-12127
**Subsystem**: CQL / Query Execution
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
Dense non-compound tables (legacy COMPACT STORAGE with a single clustering column) do not accept empty `ByteBuffer` values as clustering keys. When a query contained an EQ, IN, or slice restriction whose bound resolved to an empty `ByteBuffer`, `SelectStatement` passed it through to the storage engine unchanged. The engine either threw an error or treated the empty buffer as a valid key and returned no rows. Three code paths were affected: (1) `getPageableCommand` — the whole range/partition command was built even though the result would always be empty; (2) IN-list expansion — each IN value was dispatched individually and empty values caused server-side errors; (3) non-compound slice bounds — an empty end-bound was passed verbatim instead of being treated as inclusive (open-ended), changing the semantics of the query.

### Before / After
```java
// BEFORE – getPageableCommand, no guard
private Pageable getPageableCommand(QueryOptions options, ...) {
    int limitForQuery = updateLimitForQuery(limit);
    if (isKeyRange || usesSecondaryIndexing)
        return getRangeCommand(...);
    ...
}

// AFTER – short-circuit when empty value makes result trivially empty
private Pageable getPageableCommand(QueryOptions options, ...) {
    if (isNotReturningAnyRows(options))
        return null;
    ...
}

// BEFORE – IN value dispatched even when empty
Composite prefix = builder.buildWith(val);
columns.addAll(addSelectedColumns(prefix));

// AFTER – skip empty IN values that would cause errors
if (ignoreInValue(val))
    continue;
Composite prefix = builder.buildWith(val);
columns.addAll(addSelectedColumns(prefix));

// BEFORE – empty slice end-bound passed as-is
return type.makeCellName(sliceRestriction.bound(bound, options));

// AFTER – empty end-bound treated as inclusive (open), matching storage semantics
ByteBuffer value = ...;
return value.hasRemaining() ? type.makeCellName(value) : null;
```

---

## How to Spot
Whenever code builds storage-layer read operations from user-supplied `ByteBuffer` values, add a `hasRemaining()` guard before using the buffer as a cell name, especially in tables where the comparator is `isDense() && !isCompound()`. Missing these guards silently changes query semantics on compact-storage tables. The scrubber was also updated in this commit to handle out-of-order rows more cleanly, suggesting this class of bug was widespread in the compact-storage read path.

---

## Test
`SelectTest`, `DeleteTest`, and `SecondaryIndexTest` were extended with tests covering EQ, IN, and slice queries that include empty `ByteBuffer` values; `ReversedTypeTest` was updated to reflect the `dropData` removal.
