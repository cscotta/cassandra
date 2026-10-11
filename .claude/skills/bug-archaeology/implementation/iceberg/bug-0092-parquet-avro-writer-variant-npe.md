# Bug 0092: ParquetAvroWriter NPE when schema contains variant type

**Commit**: 56092bc8cbfa19380cdd4513fc707c6d97ef8e36 · **Ticket**: #15934

## Detection Rule

When a visitor or builder class extends a base class that adds new abstract `visit*` methods for new type variants, verify that all concrete subclasses override every new method; an unoverridden method that returns `null` or calls through to a default that returns `null` will cause an NPE at the call site.

## Code Smell

A concrete subclass of a Parquet type visitor that implements `map()`, `list()`, `primitive()`, etc. but lacks a `variant()` override — when the schema traversal reaches a variant group node, the default implementation returns `null`, which is then dereferenced by the caller.

## Tags

- scope: java
- category: missing-null-check
- code-pattern: null-after-map-lookup
- severity: crash
- subsystem: parquet/ParquetAvroWriter

## Root Cause

`ParquetTypeVisitor` added a `variant(GroupType)` method for the new Parquet variant type. `ParquetAvroWriter`'s inner `WriteBuilder` extends `ParquetTypeVisitor` but did not override `variant()`, so the default no-op implementation returned `null`. The caller then attempted to wrap that `null` result in an `option(...)` writer, triggering a NullPointerException. The fix adds an explicit override that throws `UnsupportedOperationException`, giving a clear error instead of a crash.

## Example

```java
// BEFORE — variant() not overridden; default returns null → NPE at call site
class WriteBuilder extends ParquetTypeVisitor<ParquetValueWriter<?>> {
    @Override
    public ParquetValueWriter<?> map(GroupType map, ...) { ... }
    @Override
    public ParquetValueWriter<?> primitive(PrimitiveType primitive) { ... }
    // variant() missing
}
```

```java
// AFTER
@Override
public ParquetValueWriter<?> variant(GroupType variant) {
    throw new UnsupportedOperationException("Avro writer does not support variant types");
}
```
