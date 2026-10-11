# Bug 1876: Fix Altering Column Types

**Commit**: 189a60728d · **Ticket**: CASSANDRA-6185
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/AlterTableStatement.java, src/java/org/apache/cassandra/db/marshal/AbstractType.java
**Category**: Logic error in condition

---

## Root Cause

`ALTER TABLE ... ALTER COLUMN TYPE` used `isCompatibleWith` for all column roles (partition key, value alias, regular column), but only comparator-used positions (clustering columns / column aliases) actually require order-compatibility. For partition key components and regular column values, it is sufficient that new type can decode the bytes written by the old type, a weaker guarantee captured by `isValueCompatibleWith`. Conversely, `AbstractType.isCompatibleWith` used reference equality (`this == previous`) instead of `equals`, so equal-by-value types (e.g. two `CompositeType` instances with the same component list) were incorrectly rejected. The fix introduces `isValueCompatibleWith` as a distinct override, uses the correct predicate per column role, and adds incompatibility checks with better error messages for cases that were previously silently allowed.

### Before / After

```java
// BEFORE – AbstractType uses reference equality
public boolean isCompatibleWith(AbstractType<?> previous)
{
    return this == previous;
}

// BEFORE – VALUE_ALIAS uses isCompatibleWith (too strict: rejects safe widening)
if (!validator.getType().isCompatibleWith(cfm.getDefaultValidator()))
    throw ...;

// BEFORE – COLUMN_METADATA uses isCompatibleWith (too strict)
if (!validator.getType().isCompatibleWith(column.getValidator()))
    throw ...;
```

```java
// AFTER – AbstractType uses equals; new weaker isValueCompatibleWith provided
public boolean isCompatibleWith(AbstractType<?> previous)
{
    return this.equals(previous);
}
public boolean isValueCompatibleWith(AbstractType<?> previous)
{
    return isCompatibleWith(previous);
}

// AFTER – VALUE_ALIAS and COLUMN_METADATA use the weaker predicate
if (!validator.getType().isValueCompatibleWith(cfm.getDefaultValidator()))
    throw ...;
```

---

## How to Spot

When validating a schema change, distinguish between columns that participate in ordering (clustering columns, index rows) and those used only for value storage. The former require `isCompatibleWith` (order-preserving); the latter only need `isValueCompatibleWith` (byte-decodeable). Applying the stricter check to value columns blocks valid type widening (e.g. `int` to `blob`).

---

## Test

No test added.
