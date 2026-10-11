# Bug 0166: Position delete rewrite fails on array/map columns in Spark 3.4, 3.5, and 4.0

**Commit**: 32b2f00125261b93c29aca9068d78390135a80f1 · **Ticket**: #15743

## Detection Rule

When a bug fix is applied to one version of a multi-version module (e.g., Spark 4.1), verify that all sibling version modules containing a copy of the same file have also been updated — divergent copies silently reintroduce the bug for users of the other versions.

## Code Smell

A method `nonConstantFieldIds()` in `PositionDeletesRowReader` exists in multiple versioned module directories (e.g., `spark/v3.4/`, `spark/v3.5/`, `spark/v4.0/`) with the `.isPrimitiveType()` guard still present after only the `v4.1/` copy was fixed.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: incomplete-multi-version-backport
- severity: crash
- subsystem: spark/position-deletes

## Root Cause

The fix in commit #15632 (bug 0168) updated only `spark/v4.1/spark/src/main/java/.../PositionDeletesRowReader.java`. The identical bug remained in the Spark 3.4, 3.5, and 4.0 copies of the same file. Users on those Spark versions continued to hit crashes when rewriting position delete files on tables with array or map columns.

## Example

```java
// BEFORE (still present in v3.4, v3.5, v4.0 after the v4.1 fix)
private Set<Integer> nonConstantFieldIds(Map<Integer, ?> idToConstant) {
    Set<Integer> fields = expectedSchema().idToName().keySet();
    return fields.stream()
        .filter(id -> expectedSchema().findField(id).type().isPrimitiveType())
        .filter(id -> !idToConstant.containsKey(id))
        .collect(Collectors.toSet());
}
```

```java
// AFTER (ported to v3.4, v3.5, v4.0)
int[] nonConstantFieldIds =
    expectedSchema().idToName().keySet().stream()
        .filter(id -> !idToConstant.containsKey(id))
        .mapToInt(Integer::intValue)
        .toArray();
```
