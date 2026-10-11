# Bug 0440: DeleteOrphanFiles result reports zero orphanFilesCount despite deleting files

**Commit**: 707be1a6a69d555e6601ba5b3a0acca86cc225e2 · **Ticket**: #15178 (backport of #14886)

## Detection Rule

When a result object is built with a builder that has both a list field and a derived count field, verify that the count field is explicitly set rather than relying on a default value, especially when the two fields are populated at different code sites.

## Code Smell

`ImmutableDeleteOrphanFiles.Result.builder().orphanFileLocations(orphanFileList).build()` without `.orphanFilesCount(filesCount)`, while `filesCount` is already computed and logged on the line immediately above the builder call.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: missing-field-in-builder
- severity: silent-wrong-result
- subsystem: spark/actions

## Root Cause

`DeleteOrphanFilesSparkAction` computes `filesCount` (the total number of orphan files deleted, including those not materialized in the output list when streaming is used) and logs it, but omitted `orphanFilesCount(filesCount)` when constructing the result via the Immutables builder. Callers reading `result.orphanFilesCount()` would always receive the default value (0), making the count API useless even when files were actually deleted. This was especially impactful in streaming mode where `orphanFileLocations()` is intentionally truncated.

## Example

```java
// BEFORE
LOG.info("Deleted {} orphan files", filesCount);
return ImmutableDeleteOrphanFiles.Result.builder()
    .orphanFileLocations(orphanFileList)
    .build();
```

```java
// AFTER
LOG.info("Deleted {} orphan files", filesCount);
return ImmutableDeleteOrphanFiles.Result.builder()
    .orphanFileLocations(orphanFileList)
    .orphanFilesCount(filesCount)
    .build();
```
