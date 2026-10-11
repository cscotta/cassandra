# Bug 1608: Broken Paging State with Prepared Statement

**Commit**: 35801be6f4 · **Ticket**: CASSANDRA-7120
**Subsystem**: cql3/ResultSet, transport/ResultMessage, service/pager
**Category**: State not cleaned up

---

## Root Cause

`ResultSet.Metadata` was mutable: `setHasMorePages()` mutated `flags` and `pagingState` in-place. For prepared statements, the metadata object is shared across all executions of the same statement. When a paged query set the `HAS_MORE_PAGES` flag on the shared metadata, that mutation persisted into subsequent executions — even ones that did not require paging — causing incorrect paging tokens to appear in responses.

The fix makes `Metadata` effectively immutable: `withPagingState()` now returns a new `Metadata` instance with updated flags and paging state, leaving the original (shared) object untouched.

### Before / After

```java
// BEFORE — mutates the shared metadata object in place
public Metadata setHasMorePages(PagingState pagingState) {
    if (pagingState == null) return this;
    flags.add(Flag.HAS_MORE_PAGES);  // mutates shared EnumSet
    this.pagingState = pagingState;  // mutates shared field
    return this;
}
// caller:
if (!pager.isExhausted())
    msg.result.metadata.setHasMorePages(pager.state());
return msg;
```

```java
// AFTER — returns a new immutable copy with the paging state applied
public Metadata withPagingState(PagingState pagingState) {
    if (pagingState == null) return this;
    EnumSet<Flag> newFlags = EnumSet.copyOf(flags);
    newFlags.add(Flag.HAS_MORE_PAGES);
    return new Metadata(newFlags, names, columnCount, pagingState);
}
// caller:
return pager.isExhausted() ? msg : msg.withPagingState(pager.state());
```

---

## How to Spot

Any time a mutable object is cached and shared across requests (prepared statement metadata, schema objects, configuration), look for methods that modify state in place. If a `setXxx()` method on a cached/shared object returns `this`, it is mutating shared state and will cause cross-request contamination.

---

## Test

No dedicated test added in this commit. The scenario is: execute a prepared SELECT with paging, exhaust the pages, then execute the same prepared statement again — the second response must not carry a stale `HAS_MORE_PAGES` flag or paging token.
