# Bug 0168: rewrite_position_delete_files fails when table has array/map columns

**Commit**: 60a6e7324c4b6b234882f9d464c9ddf6cbcb6bd3 · **Ticket**: #15632

## Detection Rule

When building a set of field IDs to pass to an expression-extraction utility, if the code explicitly filters to only primitive-typed fields, verify that non-primitive fields do not also need predicate push-down — silently dropping predicates on complex types can cause incorrect filter residuals or crashes.

## Code Smell

A `Set<Integer>` of field IDs is computed by iterating schema fields and applying `.filter(id -> expectedSchema().findField(id).type().isPrimitiveType())` before the set is passed to a predicate extraction call — non-primitive fields are silently excluded.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: primitive-only-type-guard
- severity: crash
- subsystem: spark/position-deletes

## Root Cause

`PositionDeletesRowReader.nonConstantFieldIds()` filtered field IDs to only primitive types before calling `ExpressionUtil.extractByIdInclusive`. The underlying implementation used an identity partition spec that called `specBuilder.identity(schema.findColumnName(id))` for each ID; identity partitioning on array/map columns is unsupported in Iceberg and throws at spec build time. Any table with array or map columns in the delete row schema caused the rewrite to fail immediately. The fix replaced the identity-spec approach with a direct field-ID visitor that accepts any type, and removed the primitive-type guard in `nonConstantFieldIds`.

## Example

```java
// BEFORE
private Set<Integer> nonConstantFieldIds(Map<Integer, ?> idToConstant) {
    Set<Integer> fields = expectedSchema().idToName().keySet();
    return fields.stream()
        .filter(id -> expectedSchema().findField(id).type().isPrimitiveType()) // drops array/map
        .filter(id -> !idToConstant.containsKey(id))
        .collect(Collectors.toSet());
}
// then: ExpressionUtil.extractByIdInclusive(..., Ints.toArray(nonConstantFieldIds))
// extractByIdInclusive internally built identitySpec -> crash on array/map column
```

```java
// AFTER
int[] nonConstantFieldIds =
    expectedSchema().idToName().keySet().stream()
        .filter(id -> !idToConstant.containsKey(id)) // no primitive-type filter
        .mapToInt(Integer::intValue)
        .toArray();
Expression residualWithoutConstants =
    ExpressionUtil.extractByIdInclusive(
        task.residual(), expectedSchema(), caseSensitive(), nonConstantFieldIds);
// extractByIdInclusive now uses a visitor that handles any type
```
