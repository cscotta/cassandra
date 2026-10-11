# Bug 0241: Equality delete schema field ordering is non-deterministic across query projections

**Commit**: f865bac7c15dada3543015b993b633760579ad88 · **Ticket**: #13873

## Detection Rule

When a schema is constructed by selecting fields from a projected query schema AND that schema is used as a cache key or compared positionally against records from another reader, verify that field ordering is canonicalized (e.g., sorted by stable ID) rather than inheriting the arbitrary projection order.

## Code Smell

A call to `TypeUtil.select(requiredSchema, ids)` whose result is used directly as a delete schema passed to `StructProjection.create()`, where `requiredSchema` field order varies by query projection. The ordering of the resulting schema is query-dependent, not table-definition-dependent.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: projection-order-dependent-schema
- severity: silent-wrong-result
- subsystem: core/delete-filter, spark/reader-cache

## Root Cause

`DeleteFilter.applyEqDeletes` built `deleteSchema` by calling `TypeUtil.select(requiredSchema, ids)`, which preserved the field order of `requiredSchema`. Because `requiredSchema` is derived from the query projection, a narrow projection (e.g., `SELECT id`) appends equality-delete columns in identifier-field definition order, while a wide projection already has them in table-schema order. When the `SparkExecutorCache` returns delete records written with one field ordering to a reader expecting another, `StructProjection` silently reads fields at wrong positional offsets, causing deletes to be applied against incorrect values (rows are not deleted when they should be).

## Example

```java
// BEFORE
Schema deleteSchema = TypeUtil.select(requiredSchema, ids);
// deleteSchema field order depends on requiredSchema (query projection)
StructProjection projectRow = StructProjection.create(requiredSchema, deleteSchema);
```

```java
// AFTER
Schema deleteSchema = TypeUtil.selectInIdOrder(requiredSchema, ids);
// fields sorted by field ID — same order regardless of projection
StructProjection projectRow = StructProjection.create(requiredSchema, deleteSchema);
```
