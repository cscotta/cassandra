# Bug 0134: Relation#toRestriction() Crashes on Frozen Map Clustering Column with DESC Order

**Commit**: 219eea33b6 · **Ticket**: CASSANDRA-19950
**Subsystem**: src/java/org/apache/cassandra/cql3
**Category**: Logic error in condition

---

## Root Cause

`Relation.toRestriction()`, `ElementExpression`, and `ColumnsExpression` checked column type identity using raw `instanceof` casts and `isCollection()` without first calling `column.type.unwrap()`. A frozen map used as a clustering column with `CLUSTERING ORDER BY (col DESC)` is stored as `ReversedType<MapType>`. The direct `instanceof MapType` check evaluated to `false`, causing incorrect error messages ("Column X cannot be used as a map") or a `ClassCastException` when casting `ReversedType` directly to `CollectionType`. The fix consistently calls `column.type.unwrap()` before any type identity test.

### Before / After

```java
// BEFORE — direct instanceof on possibly-wrapped type
checkFalse(column.type instanceof ListType, "Indexes on list entries...");
checkTrue(column.type instanceof MapType, "Column %s cannot be used as a map", column.name);
checkTrue(column.type.isMultiCell(), "Map-entry predicates on frozen map column %s are not supported", column.name);
```

```java
// AFTER — unwrap first
AbstractType<?> baseType = column.type.unwrap();
checkFalse(baseType instanceof ListType, "Indexes on list entries...");
checkTrue(baseType instanceof MapType, "Column %s cannot be used as a map", column.name);
checkTrue(baseType.isMultiCell(), "Map-entry predicates on frozen map column %s are not supported", column.name);
```

---

## How to Spot

Every place in CQL parsing and restriction building that performs type identity checks (`instanceof CollectionType`, `isCollection()`, `isMap()`, etc.) on a `ColumnMetadata.type` must first call `type.unwrap()`. A DESC clustering column wraps its real type in `ReversedType`, so direct type checks will silently mismatch. Grep for `column.type instanceof` and `column.type.is` patterns and verify each site unwraps first.

---

## Test

`SelectSingleColumnRelationTest.textInvalidMapEntryPredicate` creates a table with a frozen `map<int,int>` as a DESC clustering key and asserts that a map-element predicate produces the expected "Map-entry predicates on frozen map column ... are not supported" error rather than a wrong message or exception.
