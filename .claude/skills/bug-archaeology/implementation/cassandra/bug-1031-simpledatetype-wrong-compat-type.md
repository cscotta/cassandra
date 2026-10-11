# Bug 1031: SimpleDateType Reports Compatibility with Wrong Integer Type

**Commit**: b05fe4ffee · **Ticket**: CASSANDRA-10027
**Subsystem**: CQL type system / marshal
**Category**: Wrong constant / default value

---

## Root Cause
`SimpleDateType.isValueCompatibleWithInternal()` declared itself compatible with `IntegerType` (CQL `varint`, arbitrary-precision integer) instead of `Int32Type` (CQL `int`, 32-bit signed integer). A `date` column stores a 32-bit day-offset value, so it is wire-compatible with `int` — both are 4 bytes, fixed-width — but incompatible with `varint`, which uses variable-length encoding. The wrong constant allowed an `ALTER TABLE … ALTER col TYPE date` from a `varint` column to succeed silently, producing corrupt data when the old variable-length encoded values were reinterpreted as fixed 4-byte dates.

### Before / After
```java
// BEFORE — claims compatibility with varint (arbitrary-precision), which is wrong
@Override
public boolean isValueCompatibleWithInternal(AbstractType<?> otherType) {
    return this == otherType || otherType == IntegerType.instance;
}

// AFTER — compatible with int (32-bit fixed-width), which matches the wire format
@Override
public boolean isValueCompatibleWithInternal(AbstractType<?> otherType) {
    return this == otherType || otherType == Int32Type.instance;
}
```

---

## How to Spot
When implementing `isValueCompatibleWithInternal()` for a numeric type, cross-reference the on-disk encoding size with every candidate compatible type. `IntegerType` (varint) uses a length-prefixed variable encoding that is never binary-compatible with any fixed-width type. A quick review rule: `SimpleDateType` / `TimeType` are 4- and 8-byte fixed values respectively — only `Int32Type` (4 bytes) and `LongType` / `TimestampType` (8 bytes) share their wire layouts.

---

## Test
`AlterTest.testAlterColumnTypeToDate()` was added: it verifies that `ALTER TABLE … ALTER c1 TYPE date` succeeds when `c1` is `int` and correctly rejects the change when `c1` is `varint`.
