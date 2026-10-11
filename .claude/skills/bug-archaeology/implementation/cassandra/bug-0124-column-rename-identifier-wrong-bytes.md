# Bug 0124: Wrong ColumnIdentifier Bytes When Renaming a Column

**Commit**: f41c2e29d2 · **Ticket**: CASSANDRA-18956
**Subsystem**: src/java/org/apache/cassandra/cql3
**Category**: Wrong serialization / deserialization

---

## Root Cause

The ANTLR grammar rule for `ALTER TABLE … RENAME … TO …` parsed the target column name via the `ident` rule, which calls `ColumnIdentifier.getInterned()`. That method looks up (or creates) a `ColumnIdentifier` from a global intern cache keyed on the *column name string*, returning an existing identifier whose internal `bytes` field may belong to a completely different column or table. The resulting `ColumnIdentifier` carried incorrect `ByteBuffer` bytes and was stored directly in the rename map without going through `prepare()`. The fix introduces a new `schema_cident` grammar rule that produces a `ColumnIdentifier.Raw` (a `Literal`) instead, and calls `prepare(cfm)` at execution time, which derives the bytes correctly from the actual table metadata.

### Before / After

```antlr
// BEFORE – target name parsed as already-resolved ColumnIdentifier (interned, wrong bytes)
Map<ColumnIdentifier.Raw, ColumnIdentifier> renames = ...;
id1=cident K_TO toId1=ident { renames.put(id1, toId1); }
```

```antlr
// AFTER – target name kept as Raw and resolved against the table at execution time
Map<ColumnIdentifier.Raw, ColumnIdentifier.Raw> renames = ...;
id1=schema_cident K_TO toId1=schema_cident { renames.put(id1, toId1); }
// In AlterTableStatement.apply():
ColumnIdentifier to = entry.getValue().prepare(cfm);
```

---

## How to Spot

Grammar rules that convert column names directly into resolved `ColumnIdentifier` objects (via an intern cache) bypass the table-metadata-aware resolution step. During review, any path from the parser to persistent schema mutations should use the `*.Raw` form and defer `prepare()` to the execution phase where the current `CFMetaData`/`TableMetadata` is available; using the already-interned form is safe only for *reading* operations, not for producing identifiers that will be stored back into schema.

---

## Test

`LegacySchemaMigratorBaseTest` (extracted from `LegacySchemaMigratorTest`) covers schema migrations including renamed columns; the migration round-trip asserts that column definitions are preserved faithfully with correct names and serialized bytes.
