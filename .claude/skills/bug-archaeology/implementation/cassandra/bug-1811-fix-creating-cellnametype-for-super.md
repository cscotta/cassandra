# Bug 1811: Fix creating cellnameType for super columns

**Commit**: 4260736e1e · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/composites/CompoundSparseCellNameType.java
**Category**: Wrong constant / default value

---

## Root Cause

`CompoundSparseCellNameType` hardcoded `UTF8Type.instance` as the column-name comparator for all sparse tables. Super-column tables can use a non-UTF8 type for the sub-column name component, so the hardcoded value produced incorrect comparisons and serialization when reading super-column data. The fix introduces an explicit `columnNameType` field that is passed through constructors; regular CQL3 tables still default to `UTF8Type`, while super-column tables supply their actual name type.

### Before / After

```java
// BEFORE
private static final AbstractType<?> columnNameType = UTF8Type.instance;

public CompoundSparseCellNameType(List<AbstractType<?>> types)
{
    this(new CompoundCType(types));
}
```

```java
// AFTER
private final AbstractType<?> columnNameType; // instance field

public CompoundSparseCellNameType(List<AbstractType<?>> types)
{
    this(types, UTF8Type.instance);
}

public CompoundSparseCellNameType(List<AbstractType<?>> types, AbstractType<?> columnNameType)
{
    this(new CompoundCType(types), columnNameType);
}
```

---

## How to Spot

Static constants used as "defaults" for polymorphic type parameters are a warning sign when the class is also used to represent legacy storage formats (like super columns) that may have different type requirements. The constant should be an instance field with a sensible default, accepted via a constructor parameter.

---

## Test

No test added.
