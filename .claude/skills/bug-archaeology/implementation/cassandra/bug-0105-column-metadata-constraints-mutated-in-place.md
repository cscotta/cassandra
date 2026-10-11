# Bug 0105: ColumnMetadata Constraints Mutated In-Place, Bypassing Immutable Schema

**Commit**: acaff7bf44 · **Ticket**: CASSANDRA-20357
**Subsystem**: cql3/statements/schema, schema
**Category**: State not cleaned up

---

## Root Cause

`ColumnMetadata.setColumnConstraints` mutated the `columnConstraints` field directly on an existing `ColumnMetadata` instance, which is supposed to be immutable as part of Cassandra's schema. `TableMetadata.Builder` and `CreateTableStatement` first added columns to the builder, then mutated the resulting objects via `setColumnConstraints`. Because `ColumnMetadata` is shared across schema copies, this in-place mutation could corrupt already-stored schema objects and skip the proper `TableMetadata` rebuild that distributes the change through TCM. The fix replaces mutation with `withNewColumnConstraints`, returning a fresh `ColumnMetadata`, and routes `AlterConstraints` through `TableMetadata.Builder.alterColumnConstraints` to produce a proper epoch-tagged new table object.

### Before / After

```java
// BEFORE — mutates the existing ColumnMetadata in place
public void setColumnConstraints(ColumnConstraints constraints)
{
    constraints.validate(this);
    this.columnConstraints = constraints;   // direct field mutation
}
```

```java
// AFTER — returns an immutable replacement
public ColumnMetadata withNewColumnConstraints(ColumnConstraints constraints)
{
    constraints.validate(this);
    return new ColumnMetadata(ksName, cfName, name, type, position, kind, mask, constraints);
}
```

---

## How to Spot

Schema objects in Cassandra (e.g., `TableMetadata`, `ColumnMetadata`) must be immutable after construction; mutations must go through `unbuild()` / `Builder` / re-build cycles so that changes are epoch-stamped and replicated via TCM. Any method named `set*` on a schema value object is a red flag: verify it returns a new object rather than modifying `this`, and that callers swap the new object back into the `TableMetadata.Builder`.

---

## Test

`AlterTableWithTableConstraintValidationTest` adds tests for `ALTER TABLE ... ALTER IF EXISTS` on a non-existent column (must silently succeed) and `ALTER TABLE ... ALTER` on a missing column (must throw `InvalidRequestException`).
