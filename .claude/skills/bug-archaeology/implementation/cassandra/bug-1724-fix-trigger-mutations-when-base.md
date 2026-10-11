# Bug 1724: Trigger Mutations Fail When Base Mutation List Is Immutable

**Commit**: f7eca98a74 · **Ticket**: CASSANDRA-6790
**Subsystem**: src/java/org/apache/cassandra/service/StorageProxy.java
**Category**: Logic error in condition

---

## Root Cause

`mutateWithTriggers` receives the caller's `Collection<? extends IMutation>` and, when trigger mutations need to be appended, casts it directly to `Collection<RowMutation>` and calls `addAll` on it. If the caller passed an immutable or unmodifiable collection (e.g., `Collections.singletonList`), the `addAll` throws an `UnsupportedOperationException` at runtime and the write fails. The fix copies the collection into a new `ArrayList` before appending trigger mutations.

### Before / After

```java
// BEFORE
Collection<RowMutation> allMutations = (Collection<RowMutation>) mutations;
if (tmutations != null)
    allMutations.addAll(tmutations);
```

```java
// AFTER
Collection<RowMutation> allMutations = new ArrayList<>((Collection<RowMutation>) mutations);
if (tmutations != null)
    allMutations.addAll(tmutations);
```

---

## How to Spot

Whenever a method accepts a `Collection` parameter and then calls a mutating method (`add`, `addAll`, `remove`) on it, the caller's collection may be unmodifiable. Always copy into a new mutable collection before mutating.

---

## Test

`TriggersTest` added — exercises trigger execution via CQL insert, CQL batch, Thrift insert, and Thrift batch, asserting that the trigger-injected column (`v2=999`) is present after each write path.
