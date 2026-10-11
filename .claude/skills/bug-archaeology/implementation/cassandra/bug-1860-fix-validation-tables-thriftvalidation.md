# Bug 1860: Incorrect CQL3 Table Detection in ThriftValidation Column Name Check

**Commit**: 91a81b7cbc · **Ticket**: CASSANDRA-6140
**Subsystem**: thrift (ThriftValidation)
**Category**: Logic error in condition

---

## Root Cause

`ThriftValidation` detected CQL3 tables by manually inspecting three `CFDefinition` flags (`isComposite && !isCompact && !metadata.isSuper()`). This hand-rolled predicate did not match the authoritative `isThriftCompatible()` method on `CFMetaData`, which encapsulates the same logic more completely. As a result, some CQL3 table shapes were misidentified as Thrift-compatible, causing column name validation to accept or reject the wrong names under Thrift access.

### Before / After

```java
// BEFORE
boolean isCQL3Table = cfDef.isComposite && !cfDef.isCompact && !metadata.isSuper();
```

```java
// AFTER
boolean isCQL3Table = !metadata.isThriftCompatible();
```

---

## How to Spot

When testing whether a schema has a certain structural property, always delegate to the single authoritative method on the metadata object rather than re-deriving it from component flags. Duplicated derivation logic drifts as the codebase evolves.

---

## Test

No test added.
