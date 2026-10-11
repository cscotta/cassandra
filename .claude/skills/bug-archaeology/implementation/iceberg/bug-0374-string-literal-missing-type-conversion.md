# Bug 0374: StringLiteral cannot be converted to Fixed or Binary types, causing filter pushdown to silently fail

**Commit**: ed39cee30d4e229cb71bbcbc7faa081375fcb179 · **Ticket**: #14882

## Detection Rule

When a `Literal.to(type)` method handles a switch-case over type kinds, verify that all binary-compatible types (`FIXED`, `BINARY`) have explicit conversion paths from string literals, not just from their native types.

## Code Smell

A `switch` statement in `StringLiteral.to()` falls through to `return null` for `FIXED` and `BINARY` cases — meaning any string-typed filter predicate on a fixed or binary column returns no literal, silently dropping the filter instead of converting it.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: missing-type-conversion-case
- severity: silent-wrong-result
- subsystem: expressions/literals

## Root Cause

`StringLiteral.to(type)` lacked `FIXED` and `BINARY` cases in its type switch, so any hex-string literal used in a predicate against those column types returned `null` instead of a decoded `ByteBuffer` literal. The expression parser for remote scan planning also had no schema context to infer the target type, causing the `testBinaryInFilter` test to be disabled with a `@Disabled` annotation rather than fixed. Adding hex-decode conversion paths for both types restores correct filter pushdown.

## Example

```java
// BEFORE: no FIXED or BINARY cases — returns null, filter dropped
switch (typeId) {
  case DECIMAL:
    BigDecimal decimal = new BigDecimal(value().toString());
    return (Literal<T>) new DecimalLiteral(decimal);
  default:
    return null;
}
```

```java
// AFTER: explicit cases decode hex string into ByteBuffer
case FIXED:
  ByteBuffer buffer =
      ByteBuffer.wrap(BASE16_ENCODING.decode(value().toString().toUpperCase(Locale.ROOT)));
  Types.FixedType fixed = (Types.FixedType) type;
  if (buffer.remaining() == fixed.length()) {
    return (Literal<T>) new FixedLiteral(buffer);
  }
  return null;
case BINARY:
  return (Literal<T>) new BinaryLiteral(
      ByteBuffer.wrap(BASE16_ENCODING.decode(value().toString().toUpperCase(Locale.ROOT))));
```
