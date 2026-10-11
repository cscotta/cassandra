# Bug 1447: Fix Counters in Supercolumns During Live Upgrades From 1.2

**Commit**: b7adf9810a · **Ticket**: CASSANDRA-7188
**Subsystem**: src/java/org/apache/cassandra/db/CounterUpdateColumn.java
**Category**: Missing override of default method

---

## Root Cause

During a live rolling upgrade from Cassandra 1.2, counter updates arriving in the old supercolumn format are converted by renaming the column to strip the supercolumn prefix. The base `Column` class provides a `withUpdatedName()` method that performs this rename and returns a new `Column` instance. `CounterUpdateColumn` did not override this method, so the rename silently returned a plain `Column` rather than a `CounterUpdateColumn`. The resulting column lost its counter-update semantics and was mishandled by the counter merge logic, corrupting counter values on upgrade.

### Before / After

```java
// BEFORE — no override; base Column.withUpdatedName() returns a plain Column
```

```java
// AFTER
@Override
public CounterUpdateColumn withUpdatedName(ByteBuffer newName)
{
    return new CounterUpdateColumn(newName, value, timestamp);
}
```

---

## How to Spot

Subclasses that carry additional semantic identity (e.g. `CounterUpdateColumn` vs. `Column`) must override every factory-style method on the base class that returns `this`-type, including `withUpdatedName()`, `withUpdatedTimestamp()`, and similar. Grepping for `withUpdated` or `copy`-style methods in a base class and checking each subclass for a corresponding `@Override` is an effective audit technique. The absence of `@Override` annotations on a type hierarchy where the base class has several return-new-instance helpers is the key smell.

---

## Test

No test added.
