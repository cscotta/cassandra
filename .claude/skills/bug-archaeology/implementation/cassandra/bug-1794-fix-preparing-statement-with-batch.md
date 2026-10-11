# Bug 1794: Fix Preparing Statement with Batch and Delete from Collection

**Commit**: 09563ab2cc · **Ticket**: CASSANDRA-6607
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/BatchStatement.java
**Category**: Wrong constant / default value

---

## Root Cause

`BatchStatement.prepare()` allocated a `CFDefinition.Name[]` array to hold the bound variable metadata for the batch. When the batch contained a collection-delete operation, the bound terms were typed as `ColumnSpecification` rather than the narrower `CFDefinition.Name`, so storing them into a `CFDefinition.Name[]` caused an `ArrayStoreException` (or silent truncation) at prepare time. Widening the array type to `ColumnSpecification[]` matches the actual runtime type of every element produced by the individual statement preparers.

### Before / After

```java
// BEFORE
CFDefinition.Name[] boundNames = new CFDefinition.Name[getBoundTerms()];
return prepare(boundNames);
```

```java
// AFTER
ColumnSpecification[] boundNames = new ColumnSpecification[getBoundTerms()];
return prepare(boundNames);
```

---

## How to Spot

Whenever an array is allocated with a concrete subtype but filled by a polymorphic method that may produce supertype instances, the array type should match the declared return type of the filling method. An `ArrayStoreException` at runtime is the signal; static analysis (or using `List<ColumnSpecification>`) would catch it earlier.

---

## Test

No test added.
