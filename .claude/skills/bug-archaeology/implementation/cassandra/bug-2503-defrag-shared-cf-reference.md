# Bug 2503: Read defragmentation hoists shared ColumnFamily reference into RowMutation, causing data corruption

**Commit**: ce6460cdfc · **Ticket**: CASSANDRA-3482

## Detection Rule

When a mutable object (e.g., `ColumnFamily`) is stored by reference into a `RowMutation` for async write, and the same reference is still being used or modified by the caller, concurrent access will corrupt both the returned query result and the written data.

## Code Smell

`new RowMutation(ks, new Row(key, returnCF))` where `returnCF` is the same object being built up as the query result — mutations applied asynchronously via `applyUnsafe()` share the live result reference.

## Tags

- scope: cassandra
- category: race-condition
- code-pattern: shared-mutable-object-in-async-write
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause

In `CollationController.collectTimeOrderedData()`, the "hoist up" defragmentation path created a `RowMutation` wrapping the live `returnCF` reference (the object still being populated as the query result). When `applyUnsafe()` wrote this mutation, it raced with the code continuing to use or return `returnCF`. The fix calls `returnCF.cloneMe()` to make a defensive copy before handing the `ColumnFamily` to the `RowMutation`.

## Example

```java
// BEFORE
RowMutation rm = new RowMutation(cfs.table.name, new Row(filter.key, returnCF));
rm.applyUnsafe();
```

```java
// AFTER
RowMutation rm = new RowMutation(cfs.table.name, new Row(filter.key, returnCF.cloneMe()));
rm.applyUnsafe();
```
