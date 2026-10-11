# Bug 1448: EXECUTE request with skipMetadata=false returns no metadata

**Commit**: 0249363e89 · **Ticket**: CASSANDRA-8054
**Subsystem**: src/java/org/apache/cassandra/cql3/ResultSet.java
**Category**: State not cleaned up

---

## Root Cause

`ResultSet.Metadata` was a shared object across all executions of a prepared statement. When paging state needed to be attached, the old code created a new `Metadata` instance via `withPagingState()` — but the `ResultSetBuilder` that assembled each execution's result reused the shared metadata reference directly without copying it first. When `setSkipMetadata()` was later called on that shared object (to suppress column headers on repeat pages), the `NO_METADATA` flag was permanently stored on the shared instance. Subsequent executions with `skipMetadata=false` therefore found the flag already set and returned no column headers, even though the caller explicitly requested them.

### Before / After

```java
// BEFORE — ResultSetBuilder reuses the shared metadata object
this.resultSet = new ResultSet(getResultMetadata(), new ArrayList<>());
```

```java
// AFTER — each execution gets its own copy
this.resultSet = new ResultSet(getResultMetadata().copy(), new ArrayList<>());
```

Additionally, paging state is now set mutably in-place on the per-execution copy rather than creating yet another shared object:

```java
// BEFORE — returned a brand-new Metadata (but the original was already mutated elsewhere)
public Metadata withPagingState(PagingState pagingState) { ... return new Metadata(newFlags, ...); }

// AFTER — mutates the per-execution copy directly
public void setHasMorePages(PagingState pagingState) { flags.add(Flag.HAS_MORE_PAGES); this.pagingState = pagingState; }
```

---

## How to Spot

Whenever a mutable object (especially one holding flags or state bits) is retrieved from a cache or factory and handed to multiple consumers without a defensive copy, ask: "can any consumer modify this object's state, and would that affect other consumers?" Here the `NO_METADATA` flag needed to be per-request, but it lived on a shared prepared-statement descriptor. The fix pattern — copy-on-use at the top of `ResultSetBuilder` — is the right sentinel: any time you see a mutable shared object being handed to a builder or assembler without `.copy()`, flag it for review.

---

## Test

No test added.
