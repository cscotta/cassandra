# Bug 0298: Wrong method name in UnsupportedOperationException message

**Commit**: 2e94af5d1f7d71848518348c81d858b2209c3751 · **Ticket**: #15453

## Detection Rule

When a default interface method throws `UnsupportedOperationException` with a message that includes the method name, verify the string literal matches the actual method name rather than a different method in the same class.

## Code Smell

A `default` method throws `new UnsupportedOperationException(getClass().getName() + " does not implement <name>")` where `<name>` is a hardcoded string that differs from the surrounding method's actual name.

## Tags

- scope: universal
- category: wrong-constant
- code-pattern: wrong-string-in-exception
- severity: cosmetic
- subsystem: api/row-delta

## Root Cause

The `RowDelta.removeRows(DataFile)` default method was copy-pasted or edited from `deleteFile()`, and the exception message string was left as `"does not implement deleteFile"` instead of being updated to `"does not implement removeRows"`. When an implementor calls `removeRows` on a class that has not overridden it, the thrown exception names the wrong method, misleading the caller during debugging.

## Example

```java
// BEFORE
default RowDelta removeRows(DataFile file) {
    throw new UnsupportedOperationException(
        getClass().getName() + " does not implement deleteFile");
}
```

```java
// AFTER
default RowDelta removeRows(DataFile file) {
    throw new UnsupportedOperationException(
        getClass().getName() + " does not implement removeRows");
}
```
