# Bug 0528: LegacyLayout Throws on Unknown Column Instead of Warning

**Commit**: 31a86f891b · **Ticket**: CASSANDRA-15081
**Subsystem**: `src/java/org/apache/cassandra/db/LegacyLayout.java`
**Category**: Logic error in condition

---

## Root Cause

When Cassandra 3.x reads a legacy 2.x SSTable and encounters a cell with an unknown column name, it threw an `IllegalStateException` rather than logging a warning and continuing. In Cassandra 2.x, the original behavior was to silently skip or warn about unknown columns without aborting the read. The 3.x port of `LegacyLayout` hardened the check into a fatal throw, which broke reads of any SSTable whose schema had drifted (e.g., offline-generated SSTables or tables where a column was dropped without cleanup).

### Before / After

```java
// BEFORE
if (!metadata.ksName.equals(SchemaConstants.SYSTEM_KEYSPACE_NAME) && metadata.getDroppedColumnDefinition(e.columnName) == null)
    throw new IllegalStateException(String.format("Got cell for unknown column %s in sstable of %s.%s: ...",
                                                  metadata.ksName, metadata.cfName, UTF8Type.instance.compose(e.columnName)), e);
throw e;
```

```java
// AFTER
if (!metadata.ksName.equals(SchemaConstants.SYSTEM_KEYSPACE_NAME) && metadata.getDroppedColumnDefinition(e.columnName) == null)
    logger.warn(String.format("Got cell for unknown column %s in sstable of %s.%s: ...",
                              metadata.ksName, metadata.cfName, UTF8Type.instance.compose(e.columnName)), e);
throw e;
```

---

## How to Spot

Look for exception throws in legacy compatibility code paths that handle schema mismatches — any time an unexpected-but-recoverable condition (unknown column in an old-format file) triggers a fatal exception rather than a warning-and-skip, it breaks backward compatibility. The tell is that 2.x never threw here; the stricter behavior was added during the 3.x translation layer without checking the original semantics.

---

## Test

The test reads a legacy `ka`-format SSTable containing a cell for an unknown column and asserts the read completes without throwing.
