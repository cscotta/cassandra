# Bug 0206: Migrating bucketed Spark table silently drops bucket configuration

**Commit**: 0400f5dead809a386637cefa35bde24d6ef2fd72 · **Ticket**: #15429

## Detection Rule

When a migration or conversion action reads metadata from a source table and writes it into a different format, verify that any source attributes that have no equivalent in the target format are explicitly rejected rather than silently ignored.

## Code Smell

A validation method that checks the source table's `locationUri` and other required attributes but never inspects `bucketSpec`, allowing a bucketed Hive/Spark table to be passed through without error and causing the bucket specification to be discarded in the resulting Iceberg table.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: missing-precondition-on-incompatible-source
- severity: data-loss
- subsystem: spark/actions

## Root Cause

`BaseTableCreationSparkAction.validateSourceTable` validated that the source table had an explicit location but never checked whether the table used bucket-based partitioning. Iceberg does not model Spark's hash-bucket partitioning, so any `CLUSTERED BY … INTO N BUCKETS` definition was silently dropped after migration, leaving the resulting Iceberg table without the equivalent distribution guarantee and potentially with incorrect read results for consumers that relied on bucket colocation.

## Example

```java
// BEFORE — only location is checked; bucket spec is ignored
protected void validateSourceTable() {
    Preconditions.checkArgument(
        !sourceCatalogTable.storage().locationUri().isEmpty(),
        "Cannot create an Iceberg table from a source without an explicit location");
}
```

```java
// AFTER — bucket spec triggers an explicit error
protected void validateSourceTable() {
    Preconditions.checkArgument(
        !sourceCatalogTable.storage().locationUri().isEmpty(),
        "Cannot create an Iceberg table from a source without an explicit location");
    Preconditions.checkArgument(
        sourceCatalogTable.bucketSpec().isEmpty(),
        "Cannot create an Iceberg table from a bucketed source table: %s",
        (Object) sourceCatalogTable.bucketSpec().getOrElse(() -> null));
}
```
