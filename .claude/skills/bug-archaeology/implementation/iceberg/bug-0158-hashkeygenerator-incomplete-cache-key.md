# Bug 0158: Flink HashKeyGenerator reuses wrong KeySelector when writeParallelism or distributionMode changes

**Commit**: 027f088d99d24fbb4a3eeb14e5da44d9f9b9efc0 · **Ticket**: #15740

## Detection Rule

When a cache key class is built from a subset of the fields that influence the cached value, verify that every field that affects the produced output is included in both `equals()` and `hashCode()`.

## Code Smell

A cache key class (`SelectorKey`) has `equals()`/`hashCode()` that reference fewer fields than the constructor stores, so two logically different keys compare as equal — look for fields declared in the class but absent from `hashCode()`.

## Tags

- scope: java
- category: wrong-filter-result
- code-pattern: incomplete-cache-key-equality
- severity: silent-wrong-result
- subsystem: flink/sink/dynamic/hash-key-generator

## Root Cause

`SelectorKey.equals()` and `hashCode()` did not include `writeParallelism` or `distributionMode`, even though both fields directly determine which `KeySelector` is built. As a result, records for a table with a different write parallelism or distribution mode would reuse a `KeySelector` built for a different configuration, routing rows to the wrong partition bucket.

## Example

```java
// BEFORE
@Override
public boolean equals(Object other) {
    ...
    && Objects.equals(equalityFields, that.equalityFields); // writeParallelism and distributionMode missing
}
@Override
public int hashCode() {
    return Objects.hash(tableName, branch, schemaId, specId, schema, spec, equalityFields);
}
```

```java
// AFTER
@Override
public boolean equals(Object other) {
    ...
    && Objects.equals(equalityFields, that.equalityFields)
    && distributionMode == that.distributionMode
    && writeParallelism == that.writeParallelism;
}
@Override
public int hashCode() {
    return Objects.hash(
        tableName, branch, schemaId, specId, schema, spec,
        equalityFields, distributionMode, writeParallelism);
}
```
