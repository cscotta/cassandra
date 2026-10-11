# Bug 1900: Leftover Merge Conflict Markers in Tracing.java

**Commit**: 76cb10ca92 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/tracing
**Category**: Logic error in condition

---

## Root Cause

A three-way merge between `HEAD`, a common ancestor, and the `cassandra-1.2` branch left unresolved conflict markers (`<<<<<<<`, `|||||||`, `=======`, `>>>>>>>`) in `Tracing.java`. The conflicting block contained three alternate forms of a `RowMutation` construction and `StorageProxy.mutate` call. The resolution keeps only the correct post-refactor form that uses `mutateWithCatch` with the `RowMutation(ks, key, cf)` constructor, and drops both the old and intermediate variants.

### Before / After

```java
// BEFORE — unresolved merge conflict in source file
<<<<<<< HEAD
RowMutation mutation = new RowMutation(TRACE_KS, sessionIdBytes, cf);
StorageProxy.mutate(Arrays.asList(mutation), ConsistencyLevel.ANY);
||||||| merged common ancestors
RowMutation mutation = new RowMutation(TRACE_KS, sessionIdBytes);
mutation.add(cf);
StorageProxy.mutate(Arrays.asList(mutation), ConsistencyLevel.ANY);
=======
mutateWithCatch(new RowMutation(TRACE_KS, sessionIdBytes, cf));
>>>>>>> cassandra-1.2
```

```java
// AFTER — resolved to the correct single form
mutateWithCatch(new RowMutation(TRACE_KS, sessionIdBytes, cf));
```

---

## How to Spot

CI builds should fail to compile when conflict markers are present. A pre-commit hook or lint step that greps for `^<<<<<<<` in `.java` files prevents committed conflict markers from ever reaching the repository.

---

## Test

No test added.
