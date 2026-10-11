# Bug 2218: Composite Index Queries Miss Results Due to Wrong Restriction Array

**Commit**: 18acbef135 · **Ticket**: CASSANDRA-4796
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`buildBound` is used both to build key bounds (from `keyRestrictions`) and column name bounds (from `columnRestrictions`). The method previously referenced `columnRestrictions[name.position]` unconditionally, so when called for key-bound construction it applied the column restrictions array to key column positions. This produced wrong slice boundaries for tables with composite partition keys, causing valid rows to fall outside the computed range and be silently excluded from results.

Additionally, the method previously iterated `cfDef.columns.values()` when building key bounds, iterating over column names instead of key names. The fix makes `names` and `restrictions` explicit parameters so each call site supplies the correct collection and array.

### Before / After

```java
// BEFORE – always uses columnRestrictions regardless of call site
private ByteBuffer buildBound(Bound bound, Restriction[] restrictions,
                               ColumnNameBuilder builder, ...) {
    for (CFDefinition.Name name : cfDef.columns.values()) {
        Restriction r = columnRestrictions[name.position];
        ...
    }
}
```

```java
// AFTER – caller supplies the correct names collection and restrictions array
private static ByteBuffer buildBound(Bound bound,
                                      Collection<CFDefinition.Name> names,
                                      Restriction[] restrictions,
                                      boolean isReversed,
                                      ColumnNameBuilder builder, ...) {
    for (CFDefinition.Name name : names) {
        Restriction r = restrictions[name.position];
        ...
    }
}
```

---

## How to Spot

When a helper method that builds range bounds accesses a field from the enclosing class instead of a parameter, verify it uses the correct field for each call site. A method that serves both key-bound and column-bound contexts must not hard-code one context's data.

---

## Test

No test added.
