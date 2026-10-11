# Bug 0073: FileRange validation bypassed for negative offset/length

**Commit**: 46c1101ad8f5e13c0efd5d991d8ce22c8d7e7de9 · **Ticket**: #15922

## Detection Rule

When a constructor validates arguments using getter methods (e.g., `length()`) instead of the raw constructor parameters, verify that the getters cannot return stale field values before the fields are assigned.

## Code Smell

A `Preconditions.checkArgument` call in a constructor that invokes `this.method()` (a getter) to check a parameter before the corresponding field has been assigned — the getter returns the field's default value (0, null, etc.) rather than the incoming argument.

## Tags

- scope: universal
- category: logic-error
- code-pattern: validate-before-assign
- severity: silent-wrong-result
- subsystem: api/io

## Root Cause

`FileRange`'s constructor called `length()` and `offset()` — which read instance fields defaulted to 0 — before assigning the constructor arguments to those fields. Any negative value passed as `offset` or `length` would cause the getters to return 0, which passes `>= 0` checks, so the validation silently did nothing. The negative values were then stored in the fields unchecked.

## Example

```java
// BEFORE
public FileRange(CompletableFuture<ByteBuffer> byteBuffer, long offset, int length)
    throws EOFException {
  Preconditions.checkArgument(
      length() >= 0, "Invalid length: %s in range (must be >= 0)", length);
  Preconditions.checkArgument(
      offset() >= 0, "Invalid offset: %s in range (must be >= 0)", offset);
  this.byteBuffer = byteBuffer;
  this.offset = offset;
  this.length = length;
}
```

```java
// AFTER
public FileRange(CompletableFuture<ByteBuffer> byteBuffer, long offset, int length)
    throws EOFException {
  Preconditions.checkArgument(length >= 0, "Invalid length: %s in range (must be >= 0)", length);
  Preconditions.checkArgument(offset >= 0, "Invalid offset: %s in range (must be >= 0)", offset);
  this.byteBuffer = byteBuffer;
  this.offset = offset;
  this.length = length;
}
```
