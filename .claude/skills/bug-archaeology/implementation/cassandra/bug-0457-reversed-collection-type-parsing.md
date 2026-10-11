# Bug 0457: CQL Collection Parsing Fails on Reversed Column Type

**Commit**: 9f8d5b8d06 · **Ticket**: CASSANDRA-15814
**Subsystem**: `src/java/org/apache/cassandra/cql3/Lists.java`, `Sets.java`, `Maps.java`
**Category**: Logic error in condition

---

## Root Cause

When a frozen collection is used as a clustering key with `WITH CLUSTERING ORDER BY (col DESC)`, the column's `AbstractType` is wrapped in a `ReversedType<>` decorator. The CQL literal parsing code in `Lists`, `Sets`, and `Maps` used a direct cast and `instanceof` check against the raw collection type (e.g., `(ListType) column.type`) without first unwrapping the possible `ReversedType` layer. This caused a `ClassCastException` at parse time whenever a collection literal was used in a query predicate against a descending-order clustering column, and it also caused `instanceof` type guards to incorrectly reject valid literals.

### Before / After

```java
// BEFORE — direct cast fails when type is ReversedType<ListType>
public static ColumnSpecification valueSpecOf(ColumnSpecification column) {
    return new ColumnSpecification(..., ((ListType) column.type).getElementsType());
}

private void validateAssignableTo(...) {
    if (!(receiver.type instanceof ListType))   // false when wrapped in ReversedType
        throw new InvalidRequestException(...);
}
```

```java
// AFTER — unwrap ReversedType before casting or checking
private static AbstractType<?> unwrap(AbstractType<?> type) {
    return type.isReversed() ? unwrap(((ReversedType<?>) type).baseType) : type;
}
private static AbstractType<?> elementsType(AbstractType<?> type) {
    return ((ListType) unwrap(type)).getElementsType();
}

private void validateAssignableTo(...) {
    AbstractType<?> type = unwrap(receiver.type);
    if (!(type instanceof ListType))
        throw new InvalidRequestException(...);
}
```

---

## How to Spot

Any code that casts or does `instanceof` on a column's `AbstractType` without first calling `unwrap()` or `isReversed()` is a candidate for this failure: clustering columns with `DESC` order always carry an invisible `ReversedType` wrapper, so type comparisons against the raw collection class will silently fail or throw. Reviewers should look for direct casts to concrete type classes (`ListType`, `SetType`, `MapType`, etc.) in code paths that handle clustering columns or column specifications, and verify that the code handles the reversed-type wrapper.

---

## Test

New `testLiteralReversedList`, `testLiteralReversedSet`, and `testLiteralReversedMap` unit tests create tables with descending-order frozen collection clustering keys, insert a row using a CQL literal, and assert that a `WHERE col=<literal>` predicate correctly retrieves the row, verifying that literal parsing succeeds for reversed collection types.
