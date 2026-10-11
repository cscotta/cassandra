# Bug 0334: Scan equals/hashCode identifies different tables as equal when names match

**Commit**: 3f6d3de04961fbf7455e4e8d3ee74ff5922cc122 · **Ticket**: #15363

## Detection Rule

When an `equals()` or `hashCode()` implementation uses a resource's display name (or path) as a uniqueness key, verify that a stable unique identifier (UUID, ID) is also included, because display names alone do not guarantee identity across renames or distinct resources that share a name.

## Code Smell

`equals()` comparisons and `hashCode()` computations that include `table().name()` but omit `table().uuid()`, where the UUID is available and the objects are expected to distinguish tables unambiguously.

## Tags

- scope: universal
- category: logic-error
- code-pattern: missing-identity-field-in-equals
- severity: silent-wrong-result
- subsystem: spark/scan-equality

## Root Cause

All four Spark scan implementations (`SparkBatchQueryScan`, `SparkChangelogScan`, `SparkCopyOnWriteScan`, `SparkStagedScan`) used only the table name in `equals()` and `hashCode()`. Two distinct tables that share the same name (e.g., after a drop-and-recreate, or two catalogs with identical paths) would be treated as the same scan, causing incorrect query plan caching or reuse. The table UUID is a stable, unique identifier that must be part of the identity check.

## Example

```java
// BEFORE
return table().name().equals(that.table().name())
    && Objects.equals(branch(), that.branch())
    && readSchema().equals(that.readSchema());

// hashCode
return Objects.hash(table().name(), branch(), readSchema());
```

```java
// AFTER
return table().name().equals(that.table().name())
    && Objects.equals(table().uuid(), that.table().uuid())
    && Objects.equals(branch(), that.branch())
    && readSchema().equals(that.readSchema());

// hashCode
return Objects.hash(table().name(), table().uuid(), branch(), readSchema());
```
