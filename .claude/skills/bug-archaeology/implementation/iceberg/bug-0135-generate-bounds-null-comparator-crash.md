# Bug 0135: NPE in generateBounds when metrics mode produces null bound values

**Commit**: b086b4cbe96bd6121e1bb03630241fad84344966 · **Ticket**: #15748

## Detection Rule

When a method returns a nullable value and the return value is immediately passed to a comparator or other non-null-safe function, verify that a null check is performed before the call.

## Code Smell

Two calls to a value-generating helper (`generateBound`) whose return type can be `null` are made, and then `Comparators.forType(type).compare(value1, value2)` is called on both values without any null guard — the comparator creation and the `compare` call both assume non-null operands.

## Tags

- scope: java
- category: missing-null-check
- code-pattern: null-after-lookup
- severity: crash
- subsystem: core/test/FileGenerationUtil

## Root Cause

`generateBound` returns `null` when the `MetricsMode` is `None` or `Counts` (modes that do not record bounds). The caller `generateBounds` then unconditionally passed both values into `Comparators.forType(type).compare(...)`, causing a `NullPointerException`. The fix adds an early-return `Pair.of(null, null)` whenever either generated bound is null, before the comparator is ever created or used.

## Example

```java
// BEFORE
private static Pair<ByteBuffer, ByteBuffer> generateBounds(PrimitiveType type, MetricsMode mode) {
    Comparator<Object> cmp = Comparators.forType(type);
    Object value1 = generateBound(type, mode); // may return null
    Object value2 = generateBound(type, mode); // may return null
    if (cmp.compare(value1, value2) > 0) { // NPE when value1 or value2 is null
        ...
    }
}
```

```java
// AFTER
private static Pair<ByteBuffer, ByteBuffer> generateBounds(PrimitiveType type, MetricsMode mode) {
    Object value1 = generateBound(type, mode);
    Object value2 = generateBound(type, mode);
    if (value1 == null || value2 == null) {
        return Pair.of(null, null);
    }
    Comparator<Object> cmp = Comparators.forType(type);
    if (cmp.compare(value1, value2) > 0) {
        ...
    }
}
```
