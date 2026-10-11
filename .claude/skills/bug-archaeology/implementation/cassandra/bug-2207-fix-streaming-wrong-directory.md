# Bug 2207: Streaming Writes SSTable to Wrong Data Directory

**Commit**: 8c99a37698 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/streaming/StreamIn.java
**Category**: Wrong constant / default value

---

## Root Cause

When receiving a streamed SSTable, `StreamIn` selected a target data directory via `Directories.getLocationCapableOfSize` and then created the local descriptor with `cfStore.getTempSSTablePath(localDir.location)`. `localDir.location` is a raw filesystem path for the data directory, but `getTempSSTablePath` expects a directory path resolved through the column family's own `Directories` object to ensure the correct keyspace/table subdirectory is used. Passing the raw location caused the SSTable to be written to the data directory root rather than the correct `<keyspace>/<table>` subdirectory. The fix replaces `localDir.location` with `cfStore.directories.getLocationForDisk(localDir.location)`, which maps the disk location through the column family's directory structure.

### Before / After

```java
// BEFORE: uses raw data directory path, misses keyspace/table subdirectory
Descriptor localdesc = Descriptor.fromFilename(
    cfStore.getTempSSTablePath(localDir.location));
```

```java
// AFTER: resolves path through column family's directory mapping
Descriptor localdesc = Descriptor.fromFilename(
    cfStore.getTempSSTablePath(
        cfStore.directories.getLocationForDisk(localDir.location)));
```

---

## How to Spot

When constructing file paths for column-family-scoped data, always go through the column family's `Directories` object rather than using bare disk paths. A bare `DataDirectory.location` lacks the `<keyspace>/<table>` path components that Cassandra expects.

---

## Test

No test added.
