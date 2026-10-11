# Bug 1735: UPDATE Implicitly Modifying PRIMARY KEY Columns

**Commit**: 6137b381d2 · **Ticket**: CASSANDRA-6782
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/UpdateStatement.java
**Category**: Logic error in condition

---

## Root Cause

`UpdateStatement` shared its row-marker insertion logic with `InsertStatement` via a common code path in the parent class. The row marker — a special empty column written to ensure a row is visible even when all its non-PK columns are null — is appropriate for `INSERT` because the intent is to create a new row. For `UPDATE`, writing this marker has a surprising side-effect: it implicitly touches the primary key columns, causing `UPDATE t SET c = 1 WHERE k = 1` to behave as though the primary key itself was part of the write, violating user expectations. The fix gates the row-marker write on `StatementType.INSERT`, leaving `UPDATE` to write only the columns explicitly mentioned in the `SET` clause.

### Before / After

```java
// BEFORE: marker inserted for both INSERT and UPDATE
if (cfDef.isComposite && !cfDef.isCompact && !cfm.isSuper())
{
    ByteBuffer name = builder.copy().add(ByteBufferUtil.EMPTY_BYTE_BUFFER).build();
    cf.addColumn(params.makeColumn(name, ByteBufferUtil.EMPTY_BYTE_BUFFER));
}
```

```java
// AFTER: marker inserted only for INSERT
if (type == StatementType.INSERT && cfDef.isComposite && !cfDef.isCompact && !cfm.isSuper())
{
    ByteBuffer name = builder.copy().add(ByteBufferUtil.EMPTY_BYTE_BUFFER).build();
    cf.addColumn(params.makeColumn(name, ByteBufferUtil.EMPTY_BYTE_BUFFER));
}
```

---

## How to Spot

When a shared code path serves two distinct statement types (`INSERT` vs `UPDATE`), verify that every side-effecting operation in that path is appropriate for both types. A guard on `statementType` is commonly needed to prevent INSERT-specific semantics from leaking into UPDATE paths.

---

## Test

No test added.
