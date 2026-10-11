# Bug 0633: Missing sizeof(byte) Overload Causes Wrong DataLimits Serialized Size

**Commit**: eb05025c0a · **Ticket**: CASSANDRA-14057
**Subsystem**: `db/TypeSizes.java`
**Category**: Wrong serialization / deserialization

---

## Root Cause

`TypeSizes` provided `sizeof` overloads for `boolean`, `short`, `int`, and `long` but not for `byte`. Code computing the serialized size of `DataLimits` called `TypeSizes.sizeof(byteValue)`, which silently resolved to `sizeof(int)` (4 bytes) because of Java's implicit widening conversion from `byte` to `int`. The actual wire encoding of a `byte` is 1 byte, so the computed size was 3 bytes too large, causing a serialization size mismatch.

### Before / After

```java
// BEFORE — no byte overload; byte arguments widened to int
public static int sizeof(int value)  { return INT_SIZE; }  // INT_SIZE = 4
// sizeof((byte) x) → sizeof((int) x) → 4, should be 1
```

```java
// AFTER — explicit byte overload added
private static final int BYTE_SIZE = 1;

public static int sizeof(byte value)
{
    return BYTE_SIZE;
}
```

---

## How to Spot

Whenever a utility class exposes `sizeof` or `encodedSize` overloads for primitive types, verify that every Java primitive that may be stored on the wire has its own overload. The absence of a `byte` overload is easy to overlook because the compiler silently widens `byte` to `int`, producing no error or warning while returning an incorrect size.

---

## Test

No test added.
