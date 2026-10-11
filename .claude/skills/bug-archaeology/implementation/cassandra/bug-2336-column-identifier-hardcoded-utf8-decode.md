# Bug 2336: ColumnIdentifier constructed from ByteBuffer always decodes as UTF-8, failing for non-string comparators

**Commit**: a7ed01cc2b · **Ticket**: CASSANDRA-4269

## Detection Rule

When converting a raw `ByteBuffer` column name to a string representation for display or lookup, verify that the conversion uses the column family's declared comparator type rather than always assuming UTF-8.

## Code Smell

`new ColumnIdentifier(def.getKey())` or `new ColumnIdentifier(someByteBuffer)` anywhere that doesn't also pass the `AbstractType` comparator — the single-argument ByteBuffer constructor tries `ByteBufferUtil.string(key)` which throws `CharacterCodingException` for non-UTF-8 types like `IntegerType` or `LongType`.

## Tags

- scope: cassandra
- category: wrong-serialization
- code-pattern: hardcoded-utf8-decode-for-typed-column-name
- severity: crash
- subsystem: cql-query

## Root Cause

`CFDefinition` built `ColumnIdentifier` objects from column metadata `ByteBuffer` keys using a constructor that always decoded with `ByteBufferUtil.string(key)` (UTF-8). For tables whose comparator was a non-string type (e.g., `IntegerType`, `LongType`), this threw `CharacterCodingException` wrapped in `RuntimeException`. The fix changes the `ColumnIdentifier(ByteBuffer)` constructor to require an `AbstractType` and delegates to `type.getString(key)` for type-correct decoding.

## Example

```java
// BEFORE
public ColumnIdentifier(ByteBuffer key)
{
    this.key = key;
    this.text = ByteBufferUtil.string(key);  // always UTF-8 — fails for IntegerType etc.
}
// Usage:
ColumnIdentifier id = new ColumnIdentifier(def.getKey());  // no type passed
```

```java
// AFTER
public ColumnIdentifier(ByteBuffer key, AbstractType type)
{
    this.key = key;
    this.text = type.getString(key);  // uses the column's actual comparator type
}
// Usage:
ColumnIdentifier id = new ColumnIdentifier(def.getKey(), cfm.getColumnDefinitionComparator(def.getValue()));
```
