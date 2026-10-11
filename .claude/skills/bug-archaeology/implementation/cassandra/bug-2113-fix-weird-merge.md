# Bug 2113: Leftover merge conflict marker and missing enum comma corrupt source

**Commit**: 0467c19d9d · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cli/CliClient.java, src/java/org/apache/cassandra/config/CFMetaData.java
**Category**: Logic error in condition

---

## Root Cause

A botched merge left two defects in source files: a missing trailing comma after the `SPECULATIVE_RETRY` enum constant in `CliClient` (which would cause a compile error when `POPULATE_IO_CACHE_ON_FLUSH` was added), and a stray `<<<<<<< HEAD` merge-conflict marker in the `CFMetaData` schema string that was never removed. Both are purely mechanical artifacts of a failed merge resolution rather than a logic bug, but they would prevent compilation or produce corrupted schema strings at runtime.

### Before / After

```java
// BEFORE (CliClient.java enum)
SPECULATIVE_RETRY
POPULATE_IO_CACHE_ON_FLUSH
```

```java
// AFTER
SPECULATIVE_RETRY,
POPULATE_IO_CACHE_ON_FLUSH
```

```java
// BEFORE (CFMetaData.java)
public static final CFMetaData SchemaColumnFamiliesCf = compile(9, "CREATE TABLE ..."
-<<<<<<< HEAD
                                                                   + "keyspace_name text,"
```

```java
// AFTER
public static final CFMetaData SchemaColumnFamiliesCf = compile(9, "CREATE TABLE ..."
                                                                   + "keyspace_name text,"
```

---

## How to Spot

After any merge, run a quick `grep` for conflict markers (`<<<<<<<`, `=======`, `>>>>>>>`) before committing. Also verify that enum additions always include a trailing comma on the preceding constant.

---

## Test

No test added.
