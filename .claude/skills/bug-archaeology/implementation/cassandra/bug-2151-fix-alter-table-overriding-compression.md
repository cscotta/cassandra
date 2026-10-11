# Bug 2151: ALTER TABLE Resets Compression Options to Defaults

**Commit**: 01fcb8cac2 · **Ticket**: CASSANDRA-4996
**Subsystem**: src/java/org/apache/cassandra/cql/CFPropDefs.java, src/java/org/apache/cassandra/cql3/CFPropDefs.java, src/java/org/apache/cassandra/cql3/statements/CreateColumnFamilyStatement.java
**Category**: Wrong constant / default value

---

## Root Cause

`CFPropDefs.compressionParameters` was initialized with a double-brace initializer that unconditionally inserted the default compressor (`SnappyCompressor` if available) into the map. Because this happened at field initialization time—before any user-supplied options were parsed—`ALTER TABLE` statements that did not mention compression would still carry the default compressor entry. When those defaults were applied to the existing `CFMetaData`, they overwrote the table's actual compression configuration. The fix removes the default injection from the field initializer and instead injects the default only during `CREATE TABLE` execution, and only when the user has not specified any compression options at all.

### Before / After

```java
// BEFORE – default injected unconditionally at field init, corrupts ALTER TABLE
public final Map<String, String> compressionParameters = new HashMap<String, String>() {{
    if (CFMetaData.DEFAULT_COMPRESSOR != null)
        put(CompressionParameters.SSTABLE_COMPRESSION, CFMetaData.DEFAULT_COMPRESSOR);
}};
```

```java
// AFTER – field starts empty; default applied only for CREATE when user omitted options
public final Map<String, String> compressionParameters = new HashMap<String, String>();

// In CreateColumnFamilyStatement constructor:
if (CFMetaData.DEFAULT_COMPRESSOR != null && this.properties.compressionParameters.isEmpty())
    this.properties.compressionParameters.put(CompressionParameters.SSTABLE_COMPRESSION, CFMetaData.DEFAULT_COMPRESSOR);

// In CFPropDefs.applyToCFMetadata:
if (!compressionParameters.isEmpty())
    cfm.compressionParameters(CompressionParameters.create(compressionParameters));
```

---

## How to Spot

A shared property bag initialized with default values will corrupt update operations that legitimately omit those properties. Defaults should be applied only at create-time (and only when the user did not specify a value), never at parse-time in a class used for both CREATE and ALTER paths.

---

## Test

No test added.
