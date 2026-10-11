# Bug 0186: System Keyspace SSTable Relocation Filter Uses Exact Name Match, Misses UUID-Suffixed Directories

**Commit**: dece96f21d · **Ticket**: CASSANDRA-18963
**Subsystem**: service/CassandraDaemon
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

On startup, Cassandra relocates SSTable directories for system keyspace tables to the primary data directory, but excludes tables listed in `SystemKeyspace.TABLES_SPLIT_ACROSS_MULTIPLE_DISKS`. The filter used `contains(p.getFileName().toString())` which checked whether the set contained the exact directory name. On disk, table directories always carry a UUID suffix (e.g., `peers-37f71aca57cd11e9bf5fef248c5b7c18`), so the exact name never matched any entry in the set of bare table names. As a result, tables that should have been excluded (like `prepared_statements`) were incorrectly relocated to the first data directory on restart.

### Before / After

```java
// BEFORE
.filter(p -> !SystemKeyspace.TABLES_SPLIT_ACROSS_MULTIPLE_DISKS
                            .contains(p.getFileName()
                                       .toString()))
```

```java
// AFTER
.filter(p -> SystemKeyspace.TABLES_SPLIT_ACROSS_MULTIPLE_DISKS
                 .stream()
                 .noneMatch(t -> p.getFileName().toString().startsWith(t + '-')))
```

---

## How to Spot

Whenever filtering filesystem paths against a set of logical names, confirm that the comparison accounts for any suffix or decoration the filesystem adds to directory names. A `Set.contains(filename)` check is suspicious if the directory-naming convention appends extra components such as UUIDs.

---

## Test

Starts a single-node cluster with three data directories, spreads prepared-statement SSTables across them, restarts the node, and asserts that the SSTable data locations are unchanged after restart.
