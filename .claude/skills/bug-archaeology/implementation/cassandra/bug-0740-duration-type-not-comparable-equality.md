# Bug 0740: Duration type treated as NOT_COMPARABLE, breaking equality and slice guards

**Commit**: 6487876dde · **Ticket**: CASSANDRA-13174
**Subsystem**: CQL type system / query validation
**Category**: Logic error in condition

---

## Root Cause
`DurationType` was constructed with `ComparisonType.NOT_COMPARABLE`, which correctly prevents ordering operations but was also being used as the gate for equality comparisons. Because duration values are stored as `(months, days, nanoseconds)` triplets with no total ordering, they cannot support range slices, but they can be compared for exact equality. The `NOT_COMPARABLE` flag caused equality checks (e.g. `IF col = 1y2mo`) in lightweight transactions and `WHERE` clauses to either fail silently or produce wrong results. In addition, `SingleColumnRelation` and `ColumnCondition` checked only for `DurationType` directly — missing duration values nested inside collections, tuples, and UDTs — so slice restrictions on those composite types were not rejected with a useful error. The fix changes `DurationType` to `BYTE_ORDER` (enabling equality), adds `referencesDuration()` calls across `AbstractType`, `TupleType`, and `UserType`, and adds `Operator.isSlice()` to centralize the slice-check logic, so slice restrictions are properly rejected for any type that references a duration.

### Before / After
```java
// BEFORE — in DurationType constructor
super(ComparisonType.NOT_COMPARABLE);

// AFTER
super(ComparisonType.BYTE_ORDER);

// BEFORE — SingleColumnRelation slice guard
checkFalse(columnDef.type instanceof DurationType,
    "Slice restriction are not supported on duration columns");

// AFTER — covers nested durations too
if (columnDef.type.referencesDuration()) {
    checkFalse(columnDef.type.isCollection(), "...");
    checkFalse(columnDef.type.isTuple(), "...");
    checkFalse(columnDef.type.isUDT(), "...");
    throw invalidRequest("Slice restrictions are not supported on duration columns");
}
```

---

## How to Spot
When a type is declared `NOT_COMPARABLE` to block ordering, double-check that equality comparisons still work correctly. Also watch for type-check guards that use `instanceof` against a concrete type — they will miss the same type when it appears as a parameter of a collection, tuple, or UDT; prefer `referencesDuration()`-style virtual methods that recurse through composite types.

---

## Test
Extensive tests added in `InsertUpdateIfConditionTest` (LWT equality on duration columns), `SelectTest` (WHERE equality), and `SecondaryIndexTest` (index creation), covering plain columns, collections, tuples, and UDTs containing durations.
