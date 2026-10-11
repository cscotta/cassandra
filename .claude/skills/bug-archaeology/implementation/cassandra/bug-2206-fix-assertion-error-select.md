# Bug 2206: CQL3 SELECT Assertion Error From Inverted IN-Restriction Condition and Over-Broad Row Read

**Commit**: 3b8bff78d4 · **Ticket**: CASSANDRA-4783
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/SelectStatement.java, ModificationStatement.java, UpdateStatement.java, DeleteStatement.java
**Category**: Logic error in condition

---

## Root Cause

Two related problems existed in CQL3 statement handling. First, `SelectStatement` had an inverted boolean guard for `IN` restrictions on composite primary key parts: the condition `cfDef.isCompact && restriction.eqValues.size() > 1 && i != stmt.columnRestrictions.length - 1` rejected `IN` on non-compact tables but silently allowed it in illegal positions on compact tables, causing an assertion failure downstream. The corrected condition is `restriction.eqValues.size() > 1 && (!cfDef.isCompact || i != stmt.columnRestrictions.length - 1)`. Second, `UpdateStatement` and `DeleteStatement` performed a read of the entire clustering row when any list operation required a read-back, instead of reading only the specific columns needed. The fix collects only the column names that require reading and constructs targeted `ColumnSlice` ranges, preventing spurious data from being returned and fed into the update logic.

### Before / After

```java
// BEFORE: wrong guard — blocks non-compact, allows compact in wrong positions
else if (cfDef.isCompact && restriction.eqValues.size() > 1
         && i != stmt.columnRestrictions.length - 1)
```

```java
// AFTER: blocks IN wherever it is not the last part of the key
else if (restriction.eqValues.size() > 1
         && (!cfDef.isCompact || i != stmt.columnRestrictions.length - 1))
```

```java
// BEFORE: reads the entire row range when any list column needs reading
Map<ByteBuffer, ColumnGroupMap> rows = needsReading
    ? readRows(keys, builder, composite, local, cl) : null;
```

```java
// AFTER: reads only the specific columns identified as needing a read-back
Map<ByteBuffer, ColumnGroupMap> rows = toRead != null
    ? readRows(keys, builder, toRead, composite, local, cl) : null;
```

---

## How to Spot

Boolean conditions with mixed `&&`/`||` and negation are easy to invert. When writing "only X is allowed in position Y" logic, write a truth table or use De Morgan's law explicitly. For read-before-write optimizations, collect only the columns that need reading rather than reading the whole row.

---

## Test

No test added.
