# Bug 0587: Paxos Identity Assertions Broken After TableMetadata Made Immutable

**Commit**: 717c108374 · **Ticket**: CASSANDRA-14356
**Subsystem**: `src/java/org/apache/cassandra/service/paxos/`
**Category**: Logic error in condition

---

## Root Cause

`PaxosState` and `PrepareResponse` used reference equality (`==`) to assert that the `TableMetadata` objects carried by three different `Commit` instances all refer to the same table. When `TableMetadata` was made immutable (and thus defensively copied or re-fetched on each access), distinct object instances with identical identity could be produced for the same table, so the `==` assertions began to fail spuriously. The fix replaces reference equality with value equality on the stable `TableId`, which is the actual invariant being guarded.

### Before / After

```java
// BEFORE — reference equality fails after immutable TableMetadata
assert promised.update.metadata() == accepted.update.metadata()
    && accepted.update.metadata() == mostRecentCommit.update.metadata();
```

```java
// AFTER — compare by logical identity (TableId)
assert promised.update.metadata().id.equals(accepted.update.metadata().id)
    && accepted.update.metadata().id.equals(mostRecentCommit.update.metadata().id);
```

---

## How to Spot

Any assertion that uses `==` on mutable or value-typed objects (rather than on enums or well-known singletons) should be suspect after a refactoring that makes those objects immutable or introduces copy semantics. When `TableMetadata` changed its lifecycle, all identity checks on it needed migration from `==` to `.equals()` or id-based comparison.

---

## Test

No test added.
