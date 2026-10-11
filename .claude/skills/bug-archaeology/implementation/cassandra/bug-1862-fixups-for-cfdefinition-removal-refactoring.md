# Bug 1862: CFDefinition Removal Left Raw ByteBuffer Comparisons Instead of ColumnIdentifier.bytes

**Commit**: 144ee9a216 · **Ticket**: CASSANDRA-6253
**Subsystem**: config, cql, cql3, db (CFMetaData, AlterTableStatement, QueryProcessor, SelectStatement, ColumnFamilyStore, SecondaryIndex)
**Category**: Logic error in condition

---

## Root Cause

A large refactoring removed `CFDefinition` and replaced its per-column metadata with `ColumnDefinition` objects whose `name` field became a `ColumnIdentifier` (a wrapper around a `ByteBuffer`). Several call sites that previously compared a `ByteBuffer` against `def.name` continued to do so after the refactoring, but `ColumnIdentifier.equals(ByteBuffer)` does not match — only `ColumnIdentifier.bytes.equals(ByteBuffer)` does. Additionally, one `SelectStatement` lookup passed `def` (a `ColumnDefinition`) as the map key where `def.name` (a `ColumnIdentifier`) was required, always producing a cache miss.

### Before / After

```java
// BEFORE
if (def.name.equals(columnName))   // columnName is a ByteBuffer
```

```java
// AFTER
if (def.name.bytes.equals(columnName))
```

```java
// BEFORE (SelectStatement)
Restriction r = updateRestriction(def, stmt.metadataRestrictions.get(def), rel, names);
```

```java
// AFTER (SelectStatement)
Restriction r = updateRestriction(def, stmt.metadataRestrictions.get(def.name), rel, names);
```

---

## How to Spot

After any refactoring that wraps a primitive type (e.g. `ByteBuffer`) in a richer type (e.g. `ColumnIdentifier`), audit every `equals` call and map lookup that previously used the raw type. If the wrapper's `equals` is not symmetric with `ByteBuffer.equals`, silently wrong comparisons are left behind rather than compile errors.

---

## Test

No test added.
