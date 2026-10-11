# Bug 1486: Fix ReversedType.isCompatibleWith()

**Commit**: a4e108c40b · **Ticket**: CASSANDRA-7797
**Subsystem**: db/marshal / ReversedType
**Category**: Missing override of default method

---

## Root Cause

`ReversedType` did not override `isCompatibleWith()`, so it inherited the default implementation from `AbstractType`, which only checks whether the two types are equal by class. When a user ran `ALTER TABLE … ALTER b TYPE ReversedType(TimestampType)` on a column originally typed `ReversedType(DateType)`, the inherited implementation returned `false` even though `DateType` and `TimestampType` are wire-compatible. Conversely, it would incorrectly return `true` when comparing a `ReversedType` against a plain (non-reversed) type that happened to be the same class. The fix adds an explicit override that (a) rejects non-`ReversedType` targets outright and (b) delegates the actual compatibility check to the base types.

### Before / After

```java
// BEFORE
// No override — falls through to AbstractType.isCompatibleWith()
// which uses getClass() equality, ignoring the base type entirely.
```

```java
// AFTER
@Override
public boolean isCompatibleWith(AbstractType<?> otherType)
{
    if (!(otherType instanceof ReversedType))
        return false;

    return this.baseType.isCompatibleWith(((ReversedType) otherType).baseType);
}
```

---

## How to Spot

Wrapper / decorator types (`ReversedType`, `FrozenType`, etc.) must override every method that inspects internal structure, including compatibility and equality checks. A code review checklist for new wrapper types should include: `isCompatibleWith`, `isValueCompatibleWith`, `equals`, `hashCode`, and `comparatorSet`.

---

## Test

`TypeTest.testAlterReversedColumn`: asserts that `ALTER TABLE … ALTER b TYPE ReversedType(TimestampType)` succeeds on a column originally declared as `DateType DESC`. A second test `testIncompatibleReversedTypes` asserts that altering to `ReversedType(TimeUUIDType)` throws `ConfigurationException`. A third test `testReversedAndNonReversed` asserts that changing a non-reversed column to `ReversedType` is rejected.
