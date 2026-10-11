# Bug 1646: Fix writetime/ttl Methods for Static Columns

**Commit**: 2e9ac19a4d · **Ticket**: CASSANDRA-7081
**Subsystem**: src/java/org/apache/cassandra/cql3/statements (SelectStatement, ColumnGroupMap)
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`SelectStatement.handleGroup()` passed static column values as a `Map<CFDefinition.Name, ByteBuffer>` containing only the raw byte values, discarding the underlying `Column` objects. The `writetime()` and `ttl()` selector functions require the actual `Column` object to read its timestamp and expiration metadata. By reducing static columns to their byte values before passing them to the group handler, the code made it impossible to compute `writetime` or `ttl` for any static column — those selectors would always return null. The fix retains the full `ColumnGroupMap` for the static group and passes it directly alongside the regular group, allowing `addValue()` to call `getSimple()` which returns the raw `Column`.

### Before / After

```java
// BEFORE – static values reduced to ByteBuffer map, Column metadata lost
Map<CFDefinition.Name, ByteBuffer> staticValues = new HashMap<>();
for (CFDefinition.Name name : Iterables.filter(selection.getColumnsList(), isStaticFilter))
    staticValues.put(name, getValue(name, group));
// ...
case STATIC:
    result.add(staticValues.get(name));  // only the value bytes, no timestamp/ttl
```

```java
// AFTER – full ColumnGroupMap preserved for static group
ColumnGroupMap staticGroup = builder.firstGroup();
// ...
case STATIC:
    addValue(result, name, staticGroup);  // Column object available for writetime/ttl
```

---

## How to Spot

When pre-computing derived values from a collection of domain objects, check whether any downstream consumer needs fields beyond those captured in the derived form. Lossy pre-processing (e.g., extracting only `.value()` from a `Column`) will silently break any code that later needs `.timestamp()` or `.localDeletionTime()`.

---

## Test

No new test added in this commit; the CHANGES.txt entry references CASSANDRA-7081 where the scenario is: `SELECT writetime(col) FROM t WHERE pk = ?` for a table with static columns returns null instead of the actual write timestamp.
