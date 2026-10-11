# Bug 2096: Composite Comparator Incorrectly Applied to Super Column Families

**Commit**: 500c3f3158 · **Ticket**: CASSANDRA-5287
**Subsystem**: src/java/org/apache/cassandra/config/CFMetaData.java
**Category**: Logic error in condition

---

## Root Cause

`CFMetaData.getColumnDefinitionFromColumnName` attempted to parse the column name as a composite when the comparator was a `CompositeType`, but this logic is only valid for Standard column families. Super column families also use `CompositeType` as their comparator, but their column name structure is different. The missing check for `cfType == ColumnFamilyType.Standard` caused incorrect splitting of column names in super column families.

### Before / After

```java
// BEFORE
if (comparator instanceof CompositeType)
{
    CompositeType composite = (CompositeType)comparator;
    ByteBuffer[] components = composite.split(columnName);
```

```java
// AFTER
if (cfType == ColumnFamilyType.Standard && comparator instanceof CompositeType)
{
    CompositeType composite = (CompositeType)comparator;
    ByteBuffer[] components = composite.split(columnName);
```

---

## How to Spot

When branching on an interface type (`instanceof`), verify that the check is sufficient on its own — look for cases where multiple column family types or modes share the same comparator class but require different handling. An `instanceof` check on a shared type without a discriminating condition on the owning object's type is a red flag.

---

## Test

No test added.
