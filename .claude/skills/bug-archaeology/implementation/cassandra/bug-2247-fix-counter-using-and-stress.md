# Bug 2247: Stress Tool Counter Operations Used Wrong Column Family for CQL3

**Commit**: c5df208327 · **Ticket**: CASSANDRA-4633
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/Session.java, tools/stress/src/org/apache/cassandra/stress/operations/CqlCounterAdder.java, tools/stress/src/org/apache/cassandra/stress/operations/CqlCounterGetter.java
**Category**: Wrong constant / default value

---

## Root Cause

The stress tool's CQL counter operations (`CqlCounterAdder`, `CqlCounterGetter`) unconditionally targeted the `Counter1` column family, which was created via the Thrift API and is compatible only with CQL2. CQL3 requires a table defined via a `CREATE TABLE` DDL statement with explicit `counter` column types. When running with CQL3, queries against `Counter1` failed because that table did not exist in the CQL3 schema. The fix adds a dedicated `Counter3` table created via a CQL3 `CREATE TABLE` statement during keyspace setup, and directs CQL3 counter operations to `Counter3` while CQL2 operations continue to use `Counter1`. Additionally, hardcoded `"Keyspace1"` literals were replaced with a named constant `KEYSPACE_NAME`.

### Before / After

```java
// BEFORE — CqlCounterAdder always used Counter1
StringBuilder query = new StringBuilder(
    "UPDATE Counter1 USING CONSISTENCY " + session.getConsistencyLevel().toString() + " SET ");
```

```java
// AFTER — select CF based on CQL version
String counterCF = session.cqlVersion.startsWith("2") ? "Counter1" : "Counter3";
StringBuilder query = new StringBuilder("UPDATE ").append(wrapInQuotesIfRequired(counterCF))
                                                  .append(" USING CONSISTENCY ")
                                                  .append(session.getConsistencyLevel())
                                                  .append(" SET ");
```

---

## How to Spot

Stress or test utilities that hardcode a table name created via one API (Thrift/CQL2) will silently fail when the same utility is invoked via a different API (CQL3) that requires its own DDL-created schema. Version-conditional logic or separate setup paths are needed whenever two protocol versions use incompatible schema representations.

---

## Test

No test added.
