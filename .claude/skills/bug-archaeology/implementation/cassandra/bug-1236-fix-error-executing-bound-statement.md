# Bug 1236: Fix Error Executing Bound Statement After Adding a Collection Column

**Commit**: 63165a719c · **Ticket**: CASSANDRA-9411
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: State not cleaned up

---

## Root Cause

`SelectStatement` stored a `CFDefinition` object (a derived, cached view of table metadata) as its primary field and passed it through the call chain. `CFDefinition` is computed once at statement preparation time and cached for reuse via the prepared-statement cache. When a collection column is added to a table after a statement has been prepared, the stale `CFDefinition` no longer reflects the current schema. Because `CFDefinition` is derived from `CFMetaData` but not automatically invalidated, any execution after the schema change would use an outdated column list, producing errors when the column referenced in a bound statement was absent from the cached definition. The fix changes `SelectStatement` to store `CFMetaData` (the authoritative, live source) and obtain a fresh `CFDefinition` on each execution.

### Before / After

```java
// BEFORE: stale CFDefinition captured at prepare time, never refreshed
public final CFDefinition cfDef;
// ...
static SelectStatement forSelection(CFDefinition cfDef, Selection selection) {
    return new SelectStatement(cfDef, 0, defaultParameters, selection, null);
}
```

```java
// AFTER: store live CFMetaData; derive CFDefinition per execution
public final CFMetaData cfm;
// ...
static SelectStatement forSelection(CFMetaData cfm, Selection selection) {
    return new SelectStatement(cfm, 0, defaultParameters, selection, null);
}
// in execute():
CFDefinition cfDef = cfm.getCfDef();
```

---

## How to Spot

Any prepared or cached statement object that stores a derived/computed view of schema metadata (rather than the authoritative source) is at risk whenever DDL changes occur at runtime. Review fields of long-lived query objects and ask whether they are re-derived from a live source at execution time. Storing `CFDefinition`, schema snapshots, or computed column maps as instance fields of cached statements is the anti-pattern to flag.

---

## Test

No test added in this commit.
