# Bug 2385: CQL DELETE crashes with NPE when column family is invalid

**Commit**: b0dfb4cdc4 · **Ticket**: CASSANDRA-3755

## Detection Rule

When a method validates a resource and then calls another method that also looks up the same resource independently, the second lookup can NPE if the resource doesn't exist—call validation once and pass the result down.

## Code Smell

A method calls `validateColumnFamily(keyspace, columnFamily)` but discards the return value, then calls another method that re-fetches the same metadata via `Schema.instance.getCFMetaData()`, which returns null for a non-existent CF, and proceeds to dereference it.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: validate-then-re-fetch
- severity: crash
- subsystem: cql-query

## Root Cause

`DeleteStatement.prepareRowMutations` called `mutationForKey` which internally called `validateColumnFamily` on every loop iteration. The outer method had no guard on the schema lookup, and `Schema.instance.getCFMetaData(keyspace, columnFamily)` returned null for an invalid column family name, causing a NPE when `.getKeyValidator()` was called on the null result. The fix validates once up front and passes the `CFMetaData` object into `mutationForKey`.

## Example

```java
// BEFORE
public List<IMutation> prepareRowMutations(...) throws InvalidRequestException
{
    clientState.hasColumnFamilyAccess(columnFamily, Permission.WRITE);
    AbstractType<?> keyType = Schema.instance.getCFMetaData(keyspace, columnFamily).getKeyValidator(); // NPE if CF missing
    for (Term key : keys)
        rowMutations.add(mutationForKey(key.getByteBuffer(keyType), keyspace, timestamp, clientState));
}
```

```java
// AFTER
public List<IMutation> prepareRowMutations(...) throws InvalidRequestException
{
    CFMetaData metadata = validateColumnFamily(keyspace, columnFamily); // throws InvalidRequest if missing
    clientState.hasColumnFamilyAccess(columnFamily, Permission.WRITE);
    AbstractType<?> keyType = Schema.instance.getCFMetaData(keyspace, columnFamily).getKeyValidator();
    for (Term key : keys)
        rowMutations.add(mutationForKey(key.getByteBuffer(keyType), keyspace, timestamp, clientState, metadata));
}
```
