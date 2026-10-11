# Bug 0357: Concurrent Schema Pulls Produce Inconsistent Schema State

**Commit**: 23b61a5fa1 · **Ticket**: CASSANDRA-16856
**Subsystem**: src/java/org/apache/cassandra/schema (SchemaKeyspace), service (MigrationCoordinator, MigrationManager)
**Category**: Race condition / missing synchronization

---

## Root Cause

`SchemaKeyspace.convertSchemaToMutations()` iterated over all schema column families and assembled a `Map<DecoratedKey, Mutation>` of schema mutations, but was not synchronized.  When two threads — for example a schema push and a concurrent schema pull arriving from a peer — called this method simultaneously, they could observe an interleaved, partially-assembled mutation map that mixed schema versions, producing broken schema mutations that caused keyspaces or tables to be recreated incorrectly on receiving nodes.

### Before / After

```java
// BEFORE
public static Collection<Mutation> convertSchemaToMutations()
{
    Map<DecoratedKey, Mutation> mutationMap = new HashMap<>();
    ...
}
```

```java
// AFTER
public static synchronized Collection<Mutation> convertSchemaToMutations()
{
    Map<DecoratedKey, Mutation> mutationMap = new HashMap<>();
    ...
}
```

---

## How to Spot

Any method that reads from multiple schema column families and combines the results into a single collection is implicitly assuming that those reads form a consistent snapshot.  Without synchronization, concurrent schema mutations can interleave with the reads, producing a mixed-version output.  Methods like this that assemble schema state should be protected by a lock or should perform their reads under a single lock scope.

---

## Test

Reflection-based test that asserts the `convertSchemaToMutations` method carries the `synchronized` modifier.
