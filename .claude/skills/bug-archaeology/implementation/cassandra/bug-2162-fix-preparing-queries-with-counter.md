# Bug 2162: Preparing CQL3 queries against counter tables fails or misclassifies operations

**Commit**: ba8dfe300c · **Ticket**: CASSANDRA-5022
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/UpdateStatement.java, src/java/org/apache/cassandra/cql3/operations/PreparedOperation.java
**Category**: Logic error in condition

---

## Root Cause

During `prepare()`, the old code tried to determine `Type.COUNTER` by inspecting each operation's static type before the column family metadata was loaded — but `PreparedOperation` has no concrete type until the CF schema is known. This caused the counter/non-counter validation to either reject valid prepared counter increments or allow invalid mixed operations. The fix loads metadata first, derives `Type` from `CFMetaData.getDefaultValidator().isCommutative()` (the authoritative source), then validates each operation against that resolved type. A new `isPotentialCounterOperation()` predicate on `PreparedOperation` distinguishes `PLUS_PREPARED`/`MINUS_PREPARED` kinds so the validator can still reject non-counter operations on counter tables even for the prepared case.

### Before / After

```java
// BEFORE - type determined by iterating operations before metadata load
for (Pair<ColumnIdentifier, Operation> column : columns) {
    if (column.right.getType() == Operation.Type.COUNTER) {
        if (type == null) type = Type.COUNTER;
        else if (type != Type.COUNTER) throw ...;
    }
    else if (type == Type.COUNTER) throw ...;
}
CFMetaData metadata = validateColumnFamily(keyspace(), columnFamily(), type == Type.COUNTER);
```

```java
// AFTER - type derived from schema, then operations validated against it
CFMetaData metadata = validateColumnFamily(keyspace(), columnFamily());
type = metadata.getDefaultValidator().isCommutative() ? Type.COUNTER : Type.LOGGED;
// ... then each operation checked: PreparedOperation uses isPotentialCounterOperation()
```

---

## How to Spot

When a flag or type is derived from two separate sources (operation content vs. schema), and one source is only valid after the other is resolved, ensure the schema-authoritative source is consulted first. Validating operation intent before schema load leads to chicken-and-egg errors with prepared statements.

---

## Test

No new test added in this commit.
