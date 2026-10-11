# Bug 2449: BulkLoader reads keyspace name from wrong directory level

**Commit**: 2046956b6c · **Ticket**: CASSANDRA-3752

## Detection Rule

When inferring metadata (keyspace, table name) from a filesystem path, verify which path component corresponds to which piece of metadata, especially after directory layout changes.

## Code Smell

`this.keyspace = directory.getName()` derives the keyspace from the immediate directory (which is actually the column family directory in the new layout); the parent directory holds the keyspace name.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: wrong-path-component
- severity: crash
- subsystem: tools-clients

## Root Cause

`SSTableLoader` was written when the SSTable directory layout was `Keyspace1/` containing data files directly. After a layout change, SSTables live in `Keyspace1/Standard1/`. The constructor called `directory.getName()` which returned the column family directory name rather than the keyspace name. Using the column family directory name as the keyspace caused failed connections. The fix uses `directory.getParentFile().getName()` to retrieve the actual keyspace name.

## Example

```java
// BEFORE – wrong directory level (CF dir, not keyspace dir)
this.keyspace = directory.getName();

// AFTER – parent dir is the keyspace
this.keyspace = directory.getParentFile().getName();
```
