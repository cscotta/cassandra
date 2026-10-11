# Bug 2129: CQL3 BATCH Authorization Check Ignores Keyspace Scope

**Commit**: 3bb84e9e2a · **Ticket**: CASSANDRA-5145
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/BatchStatement.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The deduplication cache used to skip redundant authorization checks in `BatchStatement.checkAccess()` tracked only column family names (`Set<String>`), ignoring the keyspace. If a batch contained operations against tables with the same name in different keyspaces (e.g., `ks1.users` and `ks2.users`), the second authorization check would be skipped because the table name was already in the set. This meant a statement could execute against a table the client was not authorized to access.

### Before / After

```java
// BEFORE — deduplication key is column family name only
Set<String> cfamsSeen = new HashSet<String>();
if (!(cfamsSeen.contains(statement.columnFamily()))) {
    state.hasColumnFamilyAccess(statement.keyspace(), statement.columnFamily(), Permission.WRITE);
    cfamsSeen.add(statement.columnFamily());
}
```

```java
// AFTER — deduplication key is (keyspace, column family) pair
Map<String, Set<String>> cfamsSeen = new HashMap<String, Set<String>>();
// ...
Set<String> cfs = cfamsSeen.get(ks);
if (!(cfs.contains(cf))) {
    state.hasColumnFamilyAccess(ks, cf, Permission.WRITE);
    cfs.add(cf);
}
```

---

## How to Spot

Whenever a cache or deduplication set is keyed on a partial identifier (table name alone) for a resource that is actually scoped by a compound key (keyspace + table), identical short names across different scopes will incorrectly hit the cache. Review all `Set<String>` caches for table-or-column-family names and confirm they include the keyspace dimension.

---

## Test

No test added in this commit.
