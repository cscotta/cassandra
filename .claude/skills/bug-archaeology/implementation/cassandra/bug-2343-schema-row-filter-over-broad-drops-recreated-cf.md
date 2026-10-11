# Bug 2343: Schema re-creation fails because marked-for-delete CF rows are excluded from schema digest

**Commit**: 96d6aa269b · **Ticket**: CASSANDRA-4219

## Detection Rule

When filtering schema rows to determine "active" schema, check that a row is excluded only if it is BOTH marked-for-delete AND empty — a row that is marked-for-delete but still has live columns is a valid re-created schema entry and must be included.

## Code Smell

`if (row.cf == null || row.cf.isMarkedForDelete() || row.cf.isEmpty()) continue;` — the `||` means a row with any deletion marker is skipped even if it contains live columns (i.e., the CF was dropped and immediately re-created).

## Tags

- scope: cassandra
- category: wrong-filter-result
- code-pattern: over-broad-or-filter-on-deletion-flag
- severity: silent-wrong-result
- subsystem: schema-metadata

## Root Cause

When a keyspace or column family is dropped and re-created, the schema row in the system table can have a deletion marker (from the drop) while also containing live columns (from the re-create). Both `Schema.serializedSchema()` and `DefsTable.fromSchema()` used `isMarkedForDelete() || isEmpty()` to decide whether to skip a row. This caused the re-created schema to be ignored, leaving nodes with inconsistent schema views. The fix changes the condition to `isMarkedForDelete() && isEmpty()` — only skip rows that have been deleted AND have no live content.

## Example

```java
// BEFORE: skips row if marked-for-delete OR empty (misses drop+recreate)
if (row.cf == null || row.cf.isMarkedForDelete() || row.cf.isEmpty())
    continue;
```

```java
// AFTER: skips only if marked-for-delete AND nothing else present
if (row.cf == null || (row.cf.isMarkedForDelete() && row.cf.isEmpty()))
    continue;
```
