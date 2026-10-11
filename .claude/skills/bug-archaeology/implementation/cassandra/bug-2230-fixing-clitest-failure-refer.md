# Bug 2230: Integer Arithmetic Truncates Timestamp High Bits in Messaging Deserialization

**Commit**: 447fcef486 · **Ticket**: CASSANDRA-2858
**Subsystem**: src/java/org/apache/cassandra/net/IncomingTcpConnection.java
**Category**: Logic error in condition

---

## Root Cause

When reconstructing a message timestamp from the wire, the code combined the high 32 bits of the current system time with the low 30 bits read from the stream. However, `input.readInt()` returns a 32-bit `int`, and the left-shift `<< 2` was applied to that `int` before widening to `long`. Because Java arithmetic on `int` silently discards bits that overflow 32 bits, the top 2 bits of the 30-bit wire value were always lost. Casting to `long` before shifting (`(long) input.readInt() << 2`) ensures the full 32-bit value is preserved in the 64-bit result before the shift.

### Before / After

```java
// BEFORE — shift applied to int, top bits lost before widening to long
? (System.currentTimeMillis() & 0xFFFFFFFF00000000L) | ((input.readInt() << 2) >> 2)
```

```java
// AFTER — cast to long first, then shift preserves all bits
? (System.currentTimeMillis() & 0xFFFFFFFF00000000L) | (((long) input.readInt() << 2) >> 2)
```

---

## How to Spot

Whenever a bit-manipulation expression combines `int` reads with `long` arithmetic, ensure the `int` is widened to `long` before any shift that could overflow 32 bits. Look for patterns like `(someInt << N)` used in a `long` expression without an explicit cast.

---

## Test

No test added.
