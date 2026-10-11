# Bug 1741: Fix Build — Remove Spurious Checked Exception Declarations

**Commit**: 89da2c9704 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db
**Category**: Logic error in condition

---

## Root Cause

`ColumnFamilyStore.createColumnFamilyStore` (three overloads) declared `throws ConfigurationException` even though the actual incompatible-SSTable detection code only threw `RuntimeException` (not `ConfigurationException`). This caused a compilation failure at call sites that did not declare or catch `ConfigurationException`. The fix aligns the implementation — changing the thrown type to `RuntimeException` and removing the checked-exception declaration — so the signatures match what the code actually does.

### Before / After

```java
// BEFORE
public static ColumnFamilyStore createColumnFamilyStore(...) throws ConfigurationException
{
    ...
    throw new ConfigurationException(String.format("Incompatible SSTable found..."));
}
```

```java
// AFTER
public static ColumnFamilyStore createColumnFamilyStore(...)
{
    ...
    throw new RuntimeException(String.format("Incompatible SSTable found..."));
}
```

---

## How to Spot

When a method declares a checked exception but the body only throws unchecked exceptions (or a different checked exception), the signature is a lie and will cause compile errors at call sites. During review, verify that every declared checked exception type is actually thrown somewhere in the method body or its callees.

---

## Test

No test added.
