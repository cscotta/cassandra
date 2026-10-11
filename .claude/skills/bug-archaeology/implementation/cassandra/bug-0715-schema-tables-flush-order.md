# Bug 0715: Schema tables flushed in wrong order, causing startup corruption

**Commit**: 6991556e43 · **Ticket**: CASSANDRA-12213
**Subsystem**: Schema / startup
**Category**: State not cleaned up

---

## Root Cause
`SchemaKeyspace.ALL` listed the schema tables in an arbitrary order (keyspaces first, columns last). On an abrupt shutdown during flush, higher-level tables (e.g. `tables`, `keyspaces`) could be durably written while their dependents (`columns`, `types`) were still in the memtable. On the next startup, schema is loaded from disk before commit-log replay, so reading a `tables` row whose `columns` rows had not yet been flushed produced an incomplete table definition. The fix reorders `ALL` so that leaf-level tables (`columns`, `dropped_columns`, `triggers`, `types`, `functions`, `aggregates`, `indexes`) are flushed before `tables`/`views`, which are flushed before `keyspaces`. Truncation reverses this order so that parent records are removed before child records.

### Before / After
```java
// BEFORE — arbitrary order; keyspaces first
public static final List<String> ALL =
    ImmutableList.of(KEYSPACES, TABLES, COLUMNS, DROPPED_COLUMNS,
                     TRIGGERS, VIEWS, TYPES, FUNCTIONS, AGGREGATES, INDEXES);

// AFTER — dependency order; dependents first, keyspaces last
public static final ImmutableList<String> ALL =
    ImmutableList.of(COLUMNS, DROPPED_COLUMNS, TRIGGERS, TYPES,
                     FUNCTIONS, AGGREGATES, INDEXES, TABLES, VIEWS, KEYSPACES);

// Truncation now reverses the list
public static void truncate()
{
    ALL.reverse().forEach(table -> getSchemaCFS(table).truncateBlocking());
}
```

---

## How to Spot
Whenever a group of related tables must be flushed (or truncated) together and one table references data in another, the flush order must respect the dependency graph — children before parents for flush (so parents are never durable without children), and parents before children for truncation (so children are never orphaned). An unordered or alphabetically-sorted list is a red flag in this pattern.

---

## Test
No dedicated test added in this commit.
