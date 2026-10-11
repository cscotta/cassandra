# Bug 0758: Duration deserialization omits sign-consistency validation

**Commit**: 91564abadd · **Ticket**: CASSANDRA-13143
**Subsystem**: CQL / Type system / DurationSerializer
**Category**: Missing null / bounds check

---

## Root Cause
`DurationSerializer.validate(ByteBuffer bytes)` only checked that the buffer contained at least 3 bytes; it never decoded the three VInt fields (months, days, nanoseconds) and never verified that they all share the same sign. A duration like `(+2, -2, 0)` is physically invalid — months and days have opposite signs — yet it would pass validation and be persisted. The fix reads all three VInts in the validation method and asserts that they are all non-negative or all non-positive, throwing `MarshalException` with the actual values if the constraint is violated.

### Before / After
```java
// BEFORE — no sign-consistency check
public void validate(ByteBuffer bytes) throws MarshalException
{
    if (bytes.remaining() < 3)
        throw new MarshalException(...);
}

// AFTER — decode and check sign consistency
public void validate(ByteBuffer bytes) throws MarshalException
{
    if (bytes.remaining() < 3)
        throw new MarshalException(...);
    try (DataInputBuffer in = new DataInputBuffer(bytes, true))
    {
        int months = (int) in.readVInt();
        int days = (int) in.readVInt();
        long nanoseconds = in.readVInt();
        if (!((months >= 0 && days >= 0 && nanoseconds >= 0)
              || (months <= 0 && days <= 0 && nanoseconds <= 0)))
            throw new MarshalException(
                String.format("The duration months, days and nanoseconds must be all of the same sign (%d, %d, %d)",
                              months, days, nanoseconds));
    }
    catch (IOException e) { throw new AssertionError(e); }
}
```

---

## How to Spot
Validation methods that only check buffer size (or wire format) without enforcing semantic invariants on the decoded value are a common source of stored-corruption bugs. For any structured binary type with multi-field constraints (sign, range, consistency between fields), the validator should fully decode and verify those constraints — not just check that the byte count is plausible.

---

## Test
Two new `assertInvalidMessage` cases in `CreateTest.testDuration()` — one with positive months and negative days, one with negative months and positive nanoseconds — plus two `execute` cases confirming that all-positive and all-negative mixed-component durations are accepted.
