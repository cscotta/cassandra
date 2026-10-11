# Bug 2130: User-Defined Compaction Fails Against 1.1 Data Directory Layout

**Commit**: f481a39167 · **Ticket**: CASSANDRA-5118
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java
**Category**: Logic error in condition

---

## Root Cause

`CompactionManager.forceUserDefinedCompaction()` constructed a relative `File` directory using only the keyspace name (`new File(ksname)`) before parsing each SSTable filename. In the 1.1 data directory layout, SSTable files live under `<keyspace>/<columnfamily>/`, so resolving a bare filename against a keyspace-only directory produced the wrong path and caused the compaction to fail. The fix defers directory construction until after the column family name has been extracted from the filename, then builds the correct two-level path `<keyspace>/<cfname>`.

### Before / After

```java
// BEFORE — directory is just the keyspace name; wrong for 1.1 layout
File directory = new File(ksname);
// ...
Pair<Descriptor, String> p = Descriptor.fromFilename(directory, filename.trim());
```

```java
// AFTER — parse filename to extract cfname first, then build the correct directory
Descriptor desc = Descriptor.fromFilename(filename.trim());
// ... extract cfname from desc ...
File directory = new File(ksname + File.separator + cfname);
Pair<Descriptor, String> p = Descriptor.fromFilename(directory, filename.trim());
```

---

## How to Spot

When constructing paths to on-disk storage files, verify that every level of the expected directory hierarchy is included. A partial path (keyspace only, omitting column family) will silently produce a descriptor that resolves to the wrong location instead of throwing an obvious error.

---

## Test

A new `testUserDefinedCompaction` unit test in `CompactionsTest` flushes a single SSTable, submits it for user-defined compaction by filename, waits for completion, and asserts that the SSTable generation number advances by one.
