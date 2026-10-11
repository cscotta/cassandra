# Bug 0422: DecimalSerializer.toString() OOM on BigDecimal with Large Negative Exponent

**Commit**: a2aa78d7f4 · **Ticket**: CASSANDRA-14925
**Subsystem**: serializers (DecimalSerializer)
**Category**: Wrong constant / default value

---

## Root Cause

`DecimalSerializer.toString(BigDecimal)` called `BigDecimal.toPlainString()`, which expands scientific notation into a full decimal string.  A value such as `1e-(Integer.MAX_VALUE - 100)` would require generating a string with roughly 2 billion characters, causing an `OutOfMemoryError` or an extremely long GC pause.  `BigDecimal.toString()` uses scientific notation for large or small exponents and produces a compact, bounded-length string regardless of scale.

### Before / After

```java
// BEFORE – expands full decimal representation; OOM for extreme-scale values
return value == null ? "" : value.toPlainString();
```

```java
// AFTER – scientific notation; output length is bounded
return value == null ? "" : value.toString();
```

---

## How to Spot

Any call to `BigDecimal.toPlainString()` in a serialization or display path is a potential OOM vector when the value originates from user data.  `BigDecimal` allows scales up to `Integer.MAX_VALUE`; a scale of that magnitude combined with `toPlainString()` produces a string longer than any JVM can allocate.  Prefer `toString()` or `toEngineeringString()` unless plain decimal representation is a strict protocol requirement.

---

## Test

New `DecimalSerializerTest.testDecimalScaleStringify()` checks the output for both tiny and extreme-scale values (including `1e+(Integer.MAX_VALUE - 100)` and its negative counterpart) and asserts the compact scientific-notation form is produced.
