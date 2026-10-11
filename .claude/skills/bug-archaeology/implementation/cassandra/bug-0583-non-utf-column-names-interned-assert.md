# Bug 0583: Non-UTF Column Names Fail Schema Read Due to Interned Identifier Assert

**Commit**: 4b00601e83 · **Ticket**: CASSANDRA-14468
**Subsystem**: src/java/org/apache/cassandra/cql3/ColumnIdentifier.java, src/java/org/apache/cassandra/schema/SchemaKeyspace.java
**Category**: Missing null / bounds check

---

## Root Cause

When loading column definitions from the schema tables, `SchemaKeyspace` and `NativeSSTableLoaderClient` called `ColumnIdentifier.getInterned(type, bytes, text)` which interns the identifier in a global cache keyed by the type-decoded string. For columns whose names are not valid UTF-8, `type.getString(bytes)` and `row.getString("column_name")` can produce different string representations. The interning path then hit an assertion `name.isInterned()` in `ColumnDefinition`'s constructor, failing with an `AssertionError` when assertions were enabled, and potentially using an incorrect string key otherwise. The fix constructs a non-interned `ColumnIdentifier` directly from the raw bytes and the pre-stored string value, bypassing the type-decode-and-intern path entirely.

### Before / After

```java
// BEFORE
ColumnIdentifier name = ColumnIdentifier.getInterned(type,
                                                     row.getBytes("column_name_bytes"),
                                                     row.getString("column_name"));
```

```java
// AFTER
ColumnIdentifier name = new ColumnIdentifier(row.getBytes("column_name_bytes"),
                                             row.getString("column_name"));
```

---

## How to Spot

Interning helpers that derive a cache key by decoding raw bytes through a type are not safe for binary column names that are not representable in the type's encoding (e.g. arbitrary bytes stored as column names pre-CQL). Any code path that loads schema from system tables should use the stored string directly rather than re-deriving it from the binary representation.

---

## Test

No test added.
