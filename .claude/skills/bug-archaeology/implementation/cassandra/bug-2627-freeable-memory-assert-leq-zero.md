# Bug 2627: FreeableMemory finalizer asserts references == 0 instead of <= 0

**Commit**: 47cf6566bd · **Ticket**: n/a

## Detection Rule
When an `AtomicInteger` reference counter can be decremented below zero by concurrent release paths, use `<= 0` rather than `== 0` in the finalizer assertion to avoid spurious assertion failures.

## Code Smell
`assert references.get() == 0;` in a finalizer when the reference count is managed by concurrent `decrementAndGet()` calls; the count can legitimately reach negative values before finalization, causing the assertion to fire incorrectly.

## Tags
- scope: java
- category: wrong-constant
- code-pattern: overly-strict-assert
- severity: crash
- subsystem: storage-engine

## Root Cause
`FreeableMemory.finalize()` asserted `references.get() == 0`. With the reference-counting scheme where `unreference()` calls `free()` when the count hits zero, the `AtomicInteger` could already be at a negative value by finalization time (e.g. due to a double-unreference race). The strict equality assertion fired spuriously, hiding the real invariant which only requires the count to be non-positive.

## Example
```java
// BEFORE
assert references.get() == 0;

// AFTER
assert references.get() <= 0;
```
