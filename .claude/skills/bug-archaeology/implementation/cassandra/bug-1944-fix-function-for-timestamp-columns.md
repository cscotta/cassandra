# Bug 1944: dateOf() function broken for pre-2.0 timestamp columns

**Commit**: 625d43f83e · **Ticket**: CASSANDRA-5928
**Subsystem**: src/java/org/apache/cassandra/db/marshal/DateType.java
**Category**: Missing override of default method

---

## Root Cause

`DateType` is the legacy pre-2.0 type used for timestamp columns created before Cassandra 2.0. When `dateOf()` was introduced as a CQL3 function operating on timestamps, it called `asCQL3Type()` on the underlying type to determine how to interpret the value. `DateType` did not override `asCQL3Type()`, so it returned the wrong default type instead of `CQL3Type.Native.TIMESTAMP`. This caused `dateOf()` to misinterpret the value and return incorrect results for any column originally declared with the old `DateType` marshaller.

### Before / After

```java
// BEFORE — no override; falls through to AbstractType default (wrong type)
// (no asCQL3Type() method in DateType)
```

```java
// AFTER
@Override
public CQL3Type asCQL3Type()
{
    return CQL3Type.Native.TIMESTAMP;
}
```

---

## How to Spot

When a legacy type class (`DateType`, `LongType` alias, etc.) is wire-compatible with a CQL3 native type but doesn't override `asCQL3Type()`, any CQL3 function that dispatches on the CQL3 type will silently use the wrong type. Review all `AbstractType` subclasses that predate the CQL3 type system and verify they override `asCQL3Type()` appropriately.

---

## Test

No test added.
