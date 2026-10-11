# Bug 1655: fix bad merge — Remove leftover merge conflict artifacts in Tracing.java

**Commit**: a4664328c2 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/tracing
**Category**: Logic error in condition

---

## Root Cause

A merge of the `cassandra-2.0` branch into trunk left unresolved conflict markers (`<<<<<<<`, `=======`, `>>>>>>>`) inside `Tracing.java`. The conflicting region duplicated the `addParameterColumns` call and included both the old `RowMutation` type (from 2.0) and the new `Mutation` type (from trunk). The resolution keeps trunk's `Mutation` type and the `bytes()` helper for column names (from the 2.0 side), while removing the duplicated `addParameterColumns` call that appeared in the bad merge.

### Before / After

```java
// BEFORE (bad merge with conflict markers left in)
addColumn(cf, buildName(cfMeta, "coordinator"), FBUtilities.getBroadcastAddress());
addParameterColumns(cf, parameters);
<<<<<<< HEAD
addColumn(cf, buildName(cfMeta, "request"), request);
addColumn(cf, buildName(cfMeta, "started_at"), started_at);
mutateWithCatch(new Mutation(TRACE_KS, sessionIdBytes, cf));
=======
addColumn(cf, buildName(cfMeta, bytes("request")), request);
addColumn(cf, buildName(cfMeta, bytes("started_at")), started_at);
addParameterColumns(cf, parameters);
mutateWithCatch(new RowMutation(TRACE_KS, sessionIdBytes, cf));
>>>>>>> cassandra-2.0
```

```java
// AFTER
addColumn(cf, buildName(cfMeta, "coordinator"), FBUtilities.getBroadcastAddress());
addParameterColumns(cf, parameters);
addColumn(cf, buildName(cfMeta, bytes("request")), request);
addColumn(cf, buildName(cfMeta, bytes("started_at")), started_at);
mutateWithCatch(new Mutation(TRACE_KS, sessionIdBytes, cf));
```

---

## How to Spot

Before committing a merge, run `git diff --check` or search for conflict markers (`<<<<<<<`) in changed files. CI builds should reject files containing unresolved merge conflict markers.

---

## Test

No test added.
