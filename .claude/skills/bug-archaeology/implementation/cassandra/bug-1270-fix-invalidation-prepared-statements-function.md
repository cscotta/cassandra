# Bug 1270: Fix Invalidation of Prepared Statements on Function Drops

**Commit**: d9836e0efe · **Ticket**: CASSANDRA-9166
**Subsystem**: src/java/org/apache/cassandra/cql3/QueryProcessor.java (and related cql3 classes)
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When a user-defined function (UDF) or aggregate (UDA) was dropped, `QueryProcessor` was supposed to evict all prepared statements that referenced it from the prepared-statement cache. The eviction logic used a `usesFunction(ksName, functionName)` predicate that was scattered across every `Term`, `Restriction`, `Operation`, and `CQLStatement` subclass. Several concrete subclasses — including `Term.NonTerminal`, collection literals, and `UserTypes.Literal` — had stub implementations that unconditionally returned `false`, so prepared statements using those constructs with a dropped function were never invalidated. Subsequent executions of those stale statements would reference the now-deleted function and fail at runtime rather than being cleanly re-prepared. The fix replaces the fragile per-class `usesFunction` traversal with a single `getFunctions()` iterator walk: `QueryProcessor` now calls `statement.getFunctions()` and checks whether the returned iterable contains a function matching the dropped name, relying on the already-correct `getFunctions()` implementations instead of the buggy `usesFunction` ones.

### Before / After

```java
// BEFORE: stub in Term.NonTerminal silently returns false
public boolean usesFunction(String ksName, String functionName) {
    return false;  // TODO: not necessarily false for collection literals
}

// BEFORE: eviction loop
while (iterator.hasNext())
    if (iterator.next().statement.usesFunction(ksName, functionName))
        iterator.remove();
```

```java
// AFTER: eviction uses getFunctions() traversal
Iterators.removeIf(statements, statement ->
    Iterables.any(statement.statement.getFunctions(),
        f -> ksName.equals(f.name().keyspace) && functionName.equals(f.name().name)));
```

---

## How to Spot

When a feature requires every node in a composite object graph to implement a boolean predicate, prefer a single traversal over a well-maintained collection (here, `getFunctions()`) rather than distributing the predicate logic across dozens of subclasses. Stub implementations that return a hard-coded `false` with a `TODO` comment are a direct signal that the predicate approach is incomplete and will miss cases.

---

## Test

`UFTest` — extended with tests that drop a UDF used by a prepared statement and verify the statement is correctly invalidated and re-prepared on next execution.
