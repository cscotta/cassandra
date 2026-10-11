# Bug 2080: Prepared Statement Cache Collides Across Keyspaces

**Commit**: 28be383769 · **Ticket**: CASSANDRA-5352
**Subsystem**: src/java/org/apache/cassandra/cql3/QueryProcessor.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The prepared statement cache used the raw query string (or its hash) as the cache key without incorporating the current keyspace. Two identical query strings executed from different keyspaces — e.g., `SELECT * FROM users` in keyspace `ks1` vs `ks2` — produced the same cache key, so a prepare in one keyspace could return a cached statement compiled against a different keyspace's schema.

The fix concatenates the active keyspace to the query string before hashing: `keyspace + queryString`. When the keyspace is null the query must be fully qualified, so the raw string is still safe to use alone.

### Before / After

```java
// BEFORE
private static ResultMessage.Prepared storePreparedStatement(String queryString, ParsedStatement.Prepared prepared, boolean forThrift)
{
    int statementId = queryString.hashCode();          // thrift
    MD5Digest statementId = MD5Digest.compute(queryString); // native
```

```java
// AFTER
private static ResultMessage.Prepared storePreparedStatement(String queryString, String keyspace, ParsedStatement.Prepared prepared, boolean forThrift)
{
    String toHash = keyspace == null ? queryString : keyspace + queryString;
    int statementId = toHash.hashCode();               // thrift
    MD5Digest statementId = MD5Digest.compute(toHash); // native
```

---

## How to Spot

Whenever a cache key is derived from a resource name (table, column, query text) in a multi-tenant system, verify that the key includes all scope-disambiguating context (keyspace, database, schema). A cache that accepts a `clientState` but ignores its keyspace when constructing the key is a strong signal of this bug.

---

## Test

No test added.
