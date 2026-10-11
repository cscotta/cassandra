# Bug 1606: Fix countAllBatches Query

**Commit**: 3aef07dbaa · **Ticket**: CASSANDRA-7300
**Subsystem**: src/java/org/apache/cassandra/db
**Category**: Logic error in condition

---

## Root Cause

`BatchlogManager.countAllBatches()` called `executeInternal("SELECT count(*) FROM %s.%s", Keyspace.SYSTEM_KS, SystemKeyspace.BATCHLOG_CF)`. The `executeInternal` signature accepts `(String query, Object... values)` where `values` are CQL bind parameters — not `String.format()` arguments. Passing keyspace and table name as varargs attempted to bind them as CQL parameters inside the already-formatted query string, which either silently produced a wrong result or caused a runtime error. The fix pre-formats the query with `String.format()` and passes only the resulting string to `executeInternal`, matching the pattern used everywhere else in the same class.

### Before / After

```java
// BEFORE
return (int) executeInternal("SELECT count(*) FROM %s.%s", Keyspace.SYSTEM_KS, SystemKeyspace.BATCHLOG_CF).one().getLong("count");
```

```java
// AFTER
String query = String.format("SELECT count(*) FROM %s.%s", Keyspace.SYSTEM_KS, SystemKeyspace.BATCHLOG_CF);
return (int) executeInternal(query).one().getLong("count");
```

---

## How to Spot

`executeInternal(String query, Object... values)` is for CQL bind markers (`?`), not for `%s` format specifiers. Any call that passes non-empty varargs to `executeInternal` without `?` placeholders in the query string is passing format arguments where bind parameters are expected.

---

## Test

No test added.
