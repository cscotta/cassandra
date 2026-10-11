# Bug 1042: NativeSSTableLoaderClient Fails to Resolve User-Defined Types

**Commit**: 454f32dcb6 · **Ticket**: CASSANDRA-10365
**Subsystem**: SSTable tools / schema
**Category**: Missing null / bounds check

---

## Root Cause
After CASSANDRA-10365 migrated schema parsing to use `CQLTypeParser` (which requires a `Types` context for resolving user-defined types), `NativeSSTableLoaderClient` was not updated to fetch the UDT definitions from the schema tables before building `CFMetaData`. As a result, any keyspace containing UDTs would fail to load because the type resolver received an empty `Types` object. Additionally, `createDefinitionFromRow` still used the old `TypeParser.parse()` instead of `CQLTypeParser.parse()`, and did not handle descending clustering columns by wrapping the type in `ReversedType`.

### Before / After
```java
// BEFORE — no UDT fetch, old type parser, no clustering order handling
private static Map<String, CFMetaData> fetchTablesMetadata(...) { ... }
private static ColumnDefinition createDefinitionFromRow(Row row, String keyspace, String table) {
    AbstractType<?> type = TypeParser.parse(row.getString("type"));
    ...
}

// AFTER — UDTs fetched first and threaded through; new CQL type parser; clustering order applied
private static Types fetchTypes(String keyspace, Session session) { ... }
private static Map<String, CFMetaData> fetchTables(..., Types types) { ... }
private static ColumnDefinition createDefinitionFromRow(Row row, String keyspace, String table, Types types) {
    ClusteringOrder order = ClusteringOrder.valueOf(row.getString("clustering_order").toUpperCase());
    AbstractType<?> type = CQLTypeParser.parse(keyspace, row.getString("type"), types);
    if (order == ClusteringOrder.DESC)
        type = ReversedType.getInstance(type);
    ...
}
```

---

## How to Spot
Whenever a schema-reading component is updated to use a new type-resolution pipeline, all client-mode code that independently reconstructs schema (like `NativeSSTableLoaderClient`) must be audited for the same update. Look for any code that calls `TypeParser.parse()` directly after the migration to `CQLTypeParser` — those are the callers that will silently fail on UDTs or misrepresent descending clustering columns.

---

## Test
No test added.
