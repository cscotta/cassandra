# Bug 0131: referencesDuration() Returns False for Nested and Reversed Duration Types

**Commit**: e8891be04a · **Ticket**: CASSANDRA-19890
**Subsystem**: src/java/org/apache/cassandra/db/marshal
**Category**: Missing override of default method

---

## Root Cause

`AbstractType.referencesDuration()` unconditionally returned `false` in the base class, with each type that actually contains duration (e.g., `DurationType`) overriding it to return `true`. However, container types such as `ListType`, `MapType`, `TupleType`, and `UserType` also inherited the base `false` without adding the recursive subtype check, so a `list<duration>` or a UDT field of type `duration` was not recognized as referencing duration. Additionally, `ReversedType` had no override at all and returned `false` even when wrapping `DurationType` directly, breaking the check for descending clustering columns.

### Before / After

```java
// BEFORE — AbstractType base, no recursion
public boolean referencesDuration()
{
    return false;
}
// ReversedType — no override at all
```

```java
// AFTER — AbstractType recurses into subtypes
public boolean referencesDuration()
{
    for (AbstractType<?> type : subTypes())
        if (type.referencesDuration())
            return true;
    return false;
}

// ReversedType — explicit override delegates to base type
@Override
public boolean referencesDuration()
{
    return baseType.referencesDuration();
}
```

---

## How to Spot

In type hierarchies where a predicate (e.g., `referencesDuration`, `isFrozen`, `isMultiCell`) must be propagated recursively through container and wrapper types, every non-leaf type class must either delegate to its `subTypes()` loop or be explicitly overridden. Auditing all overrides of such predicates for absent `subTypes()` recursion—especially in `ReversedType`—catches this pattern.

---

## Test

`AbstractTypeTest.nestedDuration` uses property-based testing to assert that any type generated with `DurationType` as a primitive (including containers nesting it) returns `true` for `referencesDuration()`, and that wrapping it in `ReversedType` also returns `true`. A complementary test verifies that types without `DurationType` return `false`.
