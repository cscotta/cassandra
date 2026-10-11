# Bug 2299: LCS Bounds.contains Treats Single-Row SSTable as Full-Ring Range

**Commit**: 02030dd658 · **Ticket**: CASSANDRA-4411

## Detection Rule

When implementing `contains` for a closed interval `[left, right]`, handle the degenerate case where `left == right` explicitly, because a separate "wrapping range" abstraction may interpret `left == right` as a full-ring range rather than a single-point range.

## Code Smell

`Range.contains(left, right, position)` is called from `Bounds.contains` without first checking whether `left.equals(right)`; for `Range`, equal endpoints mean "the full ring", so every position appears to be contained when a single-row SSTable's first and last key are the same.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: single-element-range-mishandled
- severity: silent-wrong-result
- subsystem: compaction

## Root Cause

`Bounds<T>` models a closed interval `[left, right]`. Its `contains` implementation delegated to `Range.contains(left, right, position)`, but `Range` treats `left == right` as a full wrapping range (i.e., the entire token ring). An SSTable with exactly one row has `first == last`, so `Bounds.contains` incorrectly reported that every token fell within that SSTable's bounds, causing LCS to select wrong SSTables for compaction. The fix special-cases `left.equals(right)` — a `Bounds` with equal endpoints contains only that single point.

## Example

```java
// BEFORE — delegates to Range which wraps when left == right
public boolean contains(T position) {
    return Range.contains(left, right, position) || left.equals(position);
}
```

```java
// AFTER — single-point case handled explicitly
public boolean contains(T position) {
    return left.equals(position)
        || (!left.equals(right) && Range.contains(left, right, position));
}
```
