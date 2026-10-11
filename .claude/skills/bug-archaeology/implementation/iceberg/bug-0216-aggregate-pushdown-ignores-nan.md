# Bug 0216: MIN/MAX aggregate pushdown returns wrong results when NaN values are present in column

**Commit**: d9749048e38eb947883c5e65e05b12733d35b7d9 · **Ticket**: #15070

## Detection Rule

When reading min/max bounds from file-level metadata to answer aggregate queries without scanning data, verify that the presence of NaN values in the column is checked first, because IEEE 754 NaN is unordered and the stored lower/upper bounds may not reflect it.

## Code Smell

`hasValue()` in `MinAggregate` / `MaxAggregate` checks `lowerBounds`/`upperBounds` and value/null counts but never consults `nanValueCounts`, allowing pushdown to proceed with incorrect metadata-derived results when NaN is present.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: missing-nan-guard
- severity: silent-wrong-result
- subsystem: api/expressions/MinAggregate,MaxAggregate

## Root Cause

Iceberg stores per-column NaN counts in `DataFile.nanValueCounts()` but the `hasValue()` implementations for `MinAggregate` and `MaxAggregate` did not consult that map. When a float/double column contains NaN values the stored lower/upper bounds are derived from non-NaN values only, so pushing MIN or MAX to metadata returns a value that ignores the NaN entries, producing a wrong answer instead of the expected `NaN`.

## Example

```java
// BEFORE (MinAggregate / MaxAggregate hasValue)
protected boolean hasValue(DataFile file) {
    boolean hasBound = safeContainsKey(file.upperBounds(), fieldId);
    Long valueCount = safeGet(file.valueCounts(), fieldId);
    Long nullCount = safeGet(file.nullValueCounts(), fieldId);
    return hasBound && valueCount != null && nullCount != null && valueCount > nullCount;
}
```

```java
// AFTER
protected boolean hasValue(DataFile file) {
    // Can't determine max/min from metadata when NaN values are present
    if (containsNan(file, fieldId)) {
        return false;
    }
    boolean hasBound = safeContainsKey(file.upperBounds(), fieldId);
    Long valueCount = safeGet(file.valueCounts(), fieldId);
    Long nullCount = safeGet(file.nullValueCounts(), fieldId);
    return hasBound && valueCount != null && nullCount != null && valueCount > nullCount;
}
```
