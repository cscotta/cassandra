# Bug 1317: Fix CQLSSTableWriter Throwing Exception and Spawning Threads

**Commit**: fb67c41ad7 · **Ticket**: CASSANDRA-8808
**Subsystem**: src/java/org/apache/cassandra/io/sstable/CQLSSTableWriter.java
**Category**: Wrong constant / default value

---

## Root Cause

`CQLSSTableWriter` is a client-mode tool for bulk-loading data without a running Cassandra node, but it failed to set `Config.setClientMode(true)` before any Cassandra subsystems were initialized. As a result, `UpdateStatement#addUpdateForKey()` called `Keyspace.open()` to retrieve `SecondaryIndexManager` for index validation — a call that triggers full server-side subsystem initialization, spawning daemon threads and throwing exceptions in a context where no Cassandra node is expected to be running. The fix sets client mode in a static initializer of `CQLSSTableWriter` and adds a `validateIndexedColumns` flag to `addUpdateForKey()` so the index validation step can be skipped when writing SSTables offline.

### Before / After

```java
// BEFORE: no client-mode flag; addUpdateForKey always validates indexes via Keyspace.open()
insert.addUpdateForKey(writer.currentColumnFamily(), key, clusteringPrefix, params);
```

```java
// AFTER: client mode set at class load; index validation skipped for SSTable writes
static { Config.setClientMode(true); }
...
insert.addUpdateForKey(writer.currentColumnFamily(), key, clusteringPrefix, params, false);
```

---

## How to Spot

Utility classes that are designed for offline or client-mode use but share code with server-side paths are prone to accidental server subsystem initialization. Reviewers should check whether any code path reachable from the utility calls `Keyspace.open()`, `Schema.instance`, or other singletons that assume a running node. The absence of `Config.setClientMode(true)` at the entry point of an offline tool is the primary red flag.

---

## Test

`CQLSSTableWriterClientTest` added, verifying that the writer works in client mode without a running node and correctly produces SSTable data files.
