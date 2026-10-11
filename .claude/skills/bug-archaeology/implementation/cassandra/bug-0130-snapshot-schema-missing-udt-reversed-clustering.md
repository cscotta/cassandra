# Bug 0130: Snapshot Schema CQL Omits UDTs Used as Reversed Clustering Columns

**Commit**: 181ddc0600 · **Ticket**: CASSANDRA-20036
**Subsystem**: src/java/org/apache/cassandra/schema
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`TableMetadata.addUserTypes` collected the UDT names needed to reconstruct `CREATE TYPE` statements for a snapshot's schema. It checked `type.isUDT()` and recursed into `type.subTypes()` directly, but never unwrapped a `ReversedType` wrapper first. When a UDT was used as a clustering column with descending order, the column type is `ReversedType<UDT>`. Because `ReversedType` is not itself a UDT and `ReversedType.subTypes()` returns the base type, the guard `type.isUDT()` evaluated to `false` and the UDT was silently skipped. The generated snapshot schema therefore omitted the `CREATE TYPE` statements, making the schema unusable for restoration.

### Before / After

```java
// BEFORE — never unwraps ReversedType
private static void addUserTypes(AbstractType<?> type, Set<ByteBuffer> types)
{
    type.subTypes().forEach(t -> addUserTypes(t, types));
    if (type.isUDT())
        types.add(((UserType) type).name);
}
```

```java
// AFTER — unwrap first, then check
private static void addUserTypes(AbstractType<?> type, Set<ByteBuffer> types)
{
    AbstractType<?> unwrapped = type.unwrap();
    if (unwrapped.isUDT())
    {
        unwrapped.subTypes().forEach(t -> addUserTypes(t, types));
        types.add(((UserType) unwrapped).name);
    }
}
```

---

## How to Spot

Any code that inspects the concrete class of a column type with `instanceof` or `isUDT()` / `isCollection()` without first calling `type.unwrap()` is vulnerable to missing the `ReversedType` shell wrapping clustering columns with descending order. Review all places where type identity is tested directly rather than through the unwrapped base type.

---

## Test

`SchemaCQLHelperTest.testReversedClusteringUserTypeCQL` creates a table with a frozen UDT as a descending clustering column and asserts that all three nested `CREATE TYPE` statements appear before the `CREATE TABLE` statement in the reconstructed schema CQL.
