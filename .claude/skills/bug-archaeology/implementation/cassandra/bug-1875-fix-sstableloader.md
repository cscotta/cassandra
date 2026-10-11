# Bug 1875: Fix SSTableLoader Crash in Client Mode

**Commit**: 7159ec1004 · **Ticket**: CASSANDRA-6205
**Subsystem**: src/java/org/apache/cassandra/io/sstable/SSTableReader.java
**Category**: Missing null / bounds check

---

## Root Cause

`SSTableReader.open` initializes a `readMeter` by querying the system keyspace for per-table read rate statistics. The early-exit guard only excluded tables that belonged to the system keyspace itself, but did not account for `Config.isClientMode()`. When the SSTableLoader (which runs in client mode) opened SSTable files, it reached the code that initialises the read meter and attempted to access the system keyspace, which does not exist in client mode, causing a crash. Adding a `Config.isClientMode()` check alongside the existing system-keyspace guard makes the path skip meter initialization in client mode.

### Before / After

```java
// BEFORE – only skips meter for system keyspace tables
if (Keyspace.SYSTEM_KS.equals(desc.ksname))
{
    readMeter = null;
    return;
}
```

```java
// AFTER – also skips in client mode (e.g. SSTableLoader)
if (Keyspace.SYSTEM_KS.equals(desc.ksname) || Config.isClientMode())
{
    readMeter = null;
    return;
}
```

---

## How to Spot

Any server-side subsystem accessed during SSTable I/O (system tables, metrics, token metadata) must be guarded with `Config.isClientMode()`. The SSTableLoader uses the same code paths as the server but runs without a running Cassandra node, so any assumption about keyspace availability will fail.

---

## Test

No test added.
