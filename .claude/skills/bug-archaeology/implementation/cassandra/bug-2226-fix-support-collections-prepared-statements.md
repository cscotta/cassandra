# Bug 2226: Collection Types Unusable as Bind Parameters in Prepared Statements

**Commit**: 8b00f3a258 · **Ticket**: CASSANDRA-4739
**Subsystem**: src/java/org/apache/cassandra/cql3/operations/, src/java/org/apache/cassandra/db/marshal/
**Category**: Wrong serialization / deserialization

---

## Root Cause

When a CQL3 prepared statement used a bind marker (`?`) for a collection-typed column (list, set, or map), the server treated the bound bytes as a single opaque column value rather than deserializing and expanding them into the correct multi-column internal representation. The `UpdateStatement` dispatcher threw an error or silently wrote garbage when it encountered a `ColumnOperation` on a collection column. The fix adds `doInsertFromPrepared` methods to each collection operation class that decode the binary-serialized collection from the bound variable and write the appropriate internal column structure, and updates `UpdateStatement` to route prepared collection assignments to this new path.

### Before / After

```java
// BEFORE — in UpdateStatement, collection columns raised an exception on prepared statements
if (type == Operation.Type.COLUMN || type == Operation.Type.COUNTER)
{
    if (valueDef != null && valueDef.type.isCollection())
        throw new InvalidRequestException("Can't apply operation on column with " + valueDef.type + " type.");
    ...
}
```

```java
// AFTER — prepared collection assignments routed to new deserialization path
case COLUMN:
    if (valueDef != null && valueDef.type.isCollection())
        ((ColumnOperation)valueOperation).executePreparedCollection(cf, builder.copy(), (CollectionType)valueDef.type, params);
    else
        valueOperation.execute(cf, builder.copy(), valueDef == null ? null : valueDef.type, params);
    break;
```

---

## How to Spot

When adding a new operation path (e.g., prepared vs. non-prepared), verify that all existing type dispatchers handle the new case rather than falling through to a generic error or silently skipping collection types. Check `instanceof`/type-switch dispatchers whenever a new execution mode is added.

---

## Test

No test added.
