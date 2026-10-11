# Bug 1651: equals() always compares object to itself

**Commit**: 6b5b7f5193 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db
**Category**: Logic error in condition

---

## Root Cause

`NativeCounterCell.equals(Cell)` and `NativeExpiringCell.equals(Cell)` both contained a copy-paste error: after casting the argument to the specific subtype, they called the overloaded typed `equals()` with `this` instead of the cast parameter. This means the method always compared the object to itself, so it could never return `false` as long as the type check passed — any two counter/expiring cells of the matching type would appear equal regardless of their content.

### Before / After

```java
// BEFORE (NativeCounterCell)
public boolean equals(Cell cell)
{
    return cell instanceof CounterCell && equals((CounterCell) this);
}

// BEFORE (NativeExpiringCell)
public boolean equals(Cell cell)
{
    return cell instanceof ExpiringCell && equals((ExpiringCell) this);
}
```

```java
// AFTER (NativeCounterCell)
public boolean equals(Cell cell)
{
    return cell instanceof CounterCell && equals((CounterCell) cell);
}

// AFTER (NativeExpiringCell)
public boolean equals(Cell cell)
{
    return cell instanceof ExpiringCell && equals((ExpiringCell) cell);
}
```

---

## How to Spot

In an `equals(Object)` bridge method that delegates to a typed overload, verify the cast argument is the *parameter* variable, not `this`. Static analysis tools (e.g., SpotBugs `EQ_COMPARING_CLASS_NAMES`, `SA_LOCAL_SELF_COMPARISON`) flag self-comparisons.

---

## Test

No test added.
