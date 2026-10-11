# Bug 0694: Duration Deserialization Silently Overflows int Fields

**Commit**: 8693357109 · **Ticket**: CASSANDRA-13218
**Subsystem**: src/java/org/apache/cassandra/serializers/DurationSerializer.java
**Category**: Missing null / bounds check

---

## Root Cause

Duration values are stored on the wire as three vint-encoded fields: months, days, and nanoseconds. The months and days fields are logically 32-bit signed integers, but `readVInt()` returns a `long`. The original deserializer cast the `long` results directly to `int` without any range check, so a crafted or corrupt duration where months or days exceeded `Integer.MAX_VALUE` would silently truncate the high bits, producing a wrong value with no error. The fix reads both fields into `long` variables, validates that each fits in an `int` with a `canBeCastToInt()` helper, and only then casts.

### Before / After

```java
// BEFORE — silent overflow
int months = (int) in.readVInt();
int days   = (int) in.readVInt();
long nanoseconds = in.readVInt();
```

```java
// AFTER — explicit range validation
long monthsAsLong = in.readVInt();
long daysAsLong   = in.readVInt();
long nanoseconds  = in.readVInt();

if (!canBeCastToInt(monthsAsLong))
    throw new MarshalException("The duration months must be a 32 bits integer but was: " + monthsAsLong);
if (!canBeCastToInt(daysAsLong))
    throw new MarshalException("The duration days must be a 32 bits integer but was: " + daysAsLong);
int months = (int) monthsAsLong;
int days   = (int) daysAsLong;
```

---

## How to Spot

Any cast from a wider integer type to a narrower one without a preceding range check is a potential silent overflow. This is especially risky in serialization/deserialization code where wire values come from untrusted sources. Automated static-analysis rules that flag `(int) longValue` casts outside of explicit `Math.toIntExact()` or equivalent guards would catch this class of bug.

---

## Test

`CreateTest` was extended with a CQL test that attempts to insert a duration with months or days values exceeding `Integer.MAX_VALUE` and asserts that a `MarshalException` is thrown.
