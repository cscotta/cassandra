# Bug 0299: WRITETIME and TTL Functions Incorrectly Forbidden on Frozen Collections

**Commit**: 09692d5a58 · **Ticket**: CASSANDRA-17628
**Subsystem**: src/java/org/apache/cassandra/cql3/selection/Selectable.java
**Category**: Logic error in condition

---

## Root Cause

The guard in `Selectable` that rejects `WRITETIME` and `TTL` selection functions used `type.isCollection()` as its predicate. This was too broad: it rejected both non-frozen (multi-cell) collections (where the rejection is correct, since each element has its own timestamp) and frozen collections (where the rejection is wrong, because a frozen collection is stored as a single cell with a single timestamp). The correct predicate is `type.isMultiCell()`, which is `true` only for non-frozen collections and non-frozen UDTs—the types that genuinely lack a unified write time.

### Before / After

```java
// BEFORE
if (def.type.isCollection())
    throw new InvalidRequestException(String.format("Cannot use selection function %s on collections",
                                                    isWritetime ? "writeTime" : "ttl"));
```

```java
// AFTER
if (def.type.isMultiCell())
    throw new InvalidRequestException(String.format("Cannot use selection function %s on non-frozen collection %s",
                                                    isWritetime ? "writeTime" : "ttl",
                                                    def.name));
```

---

## How to Spot

When validating operations on collection-typed columns, `isCollection()` and `isMultiCell()` are not equivalent. `isCollection()` returns `true` for both frozen and non-frozen collections; `isMultiCell()` returns `true` only for non-frozen collections and non-frozen UDTs that are actually stored as multiple cells. Any validation that intends to target only the multi-cell storage model must use `isMultiCell()` rather than `isCollection()`.

---

## Test

`WritetimeOrTTLTest` adds dedicated tests for lists, sets, maps (both frozen and non-frozen) and frozen UDTs; it asserts that `WRITETIME`/`TTL` on non-frozen collections throw `InvalidRequestException` and that the same functions succeed on frozen variants.
