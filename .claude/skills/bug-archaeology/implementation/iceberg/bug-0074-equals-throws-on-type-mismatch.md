# Bug 0074: StructLikeWrapper.equals throws exception on mismatched partition types

**Commit**: 0babf7d171d45fd0b2ab7de55f5002c2fbe0cf61 · **Ticket**: n/a

## Detection Rule

When an `equals()` method delegates to a comparator or external utility that may throw on incompatible types, verify that type-incompatibility between the two operands is either prevented by the class invariant or caught so that `equals` returns `false` instead of propagating.

## Code Smell

An `equals()` override that calls `comparator.compare(this.struct, that.struct)` without any guard for the case where `this` and `that` were constructed with different schema types, allowing a `RuntimeException` to escape from an `equals` call.

## Tags

- scope: java
- category: logic-error
- code-pattern: exception-from-equals
- severity: crash
- subsystem: core/util

## Root Cause

`StructLikeWrapper.equals` delegates comparison to a `Comparator<StructLike>` that is built for the specific struct type of `this`. When another `StructLikeWrapper` was constructed with a different struct type (e.g., comparing `PartitionData` with an `IntegerType` field against one with a `StringType` field), the comparator would attempt a type-unsafe cast and throw a `RuntimeException`. Java's `equals` contract requires the method to return `false` for incompatible operands, not throw.

## Example

```java
// BEFORE
public boolean equals(Object other) {
  if (this == other) return true;
  if (!(other instanceof StructLikeWrapper)) return false;
  StructLikeWrapper that = (StructLikeWrapper) other;
  if (this.struct == null && that.struct == null) return true;
  if (this.struct == null || that.struct == null) return false;
  return comparator.compare(this.struct, that.struct) == 0;
}
```

```java
// AFTER
public boolean equals(Object other) {
  if (this == other) return true;
  if (!(other instanceof StructLikeWrapper)) return false;
  StructLikeWrapper that = (StructLikeWrapper) other;
  if (this.struct == null && that.struct == null) return true;
  if (this.struct == null || that.struct == null) return false;
  try {
    return comparator.compare(this.struct, that.struct) == 0;
  } catch (RuntimeException e) {
    return false;
  }
}
```
