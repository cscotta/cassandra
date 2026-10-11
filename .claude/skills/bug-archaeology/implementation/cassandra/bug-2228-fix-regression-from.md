# Bug 2228: Schema Loading Processes System Keyspace Rows, Causing Corruption on Startup

**Commit**: 7833db2605 · **Ticket**: CASSANDRA-4416
**Subsystem**: src/java/org/apache/cassandra/db/DefsTable.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

A prior change (#4416) began persisting schema rows for all keyspaces including system keyspaces into the schema column family. When `DefsTable.loadFromTable()` iterates these rows to rebuild `KSMetaData`, it did not skip rows that belonged to system keyspaces. As a result, system keyspace metadata would be constructed from the schema table and added to the live schema alongside the hard-coded definitions, causing duplicates or incorrect state. The fix adds an `ignoredSchemaRow()` predicate that filters out any row whose key matches a known system keyspace name.

### Before / After

```java
// BEFORE
if (invalidSchemaRow(row))
    continue;
```

```java
// AFTER
if (invalidSchemaRow(row) || ignoredSchemaRow(row))
    continue;
```

---

## How to Spot

Whenever a loop loads all rows from a shared table and constructs live objects, ensure there is an explicit skip for well-known or reserved entries that should not be reconstructed from persisted data (e.g., system keyspaces).

---

## Test

No test added.
