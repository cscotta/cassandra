# Bug 2569: GCInspector divide-by-zero from Long autoboxing comparison

**Commit**: 71ebcafb49 · **Ticket**: CASSANDRA-3164

## Detection Rule
When two `Long` (boxed) values are compared with `==`, the comparison checks reference identity rather than numeric equality; values outside the JVM's Long cache range (-128 to 127) will always be unequal, causing incorrect control flow.

## Code Smell
`if (count == previousCount)` where both variables are `Long` (autoboxed) instead of `long` primitives; the branch intended to skip unchanged GC counts never fires for large collection counts, causing division by `(count - previousCount)` where the denominator is unexpectedly zero elsewhere in the logic.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: boxed-Long-reference-equality
- severity: crash
- subsystem: config

## Root Cause
`GCInspector` stored GC collection counts as `Long` objects and compared them with `==`. For `Long` values above 127, the JVM does not cache instances, so two `Long` objects with the same numeric value are distinct references. The `count == previousCount` check never returned `true` for large GC counts, so the code always proceeded to compute GC statistics even when no GC had occurred, leading to a divide-by-zero in the elapsed-time calculation. Using `.equals()` fixes the comparison.

## Example
```java
// BEFORE: reference equality on boxed Long
Long count = gc.getCollectionCount();
if (count == previousCount)   // always false above 127
    continue;
```
```java
// AFTER: value equality
if (count.equals(previousCount))
    continue;
```
