# Bug 2144: crc_check_chance Not Applied to Existing SSTables on ALTER

**Commit**: 1a66ee9a94 · **Ticket**: CASSANDRA-5053
**Subsystem**: src/java/org/apache/cassandra/io/compress/CompressionParameters.java, src/java/org/apache/cassandra/db/ColumnFamilyStore.java
**Category**: Wrong constant / default value

---

## Root Cause

`crc_check_chance` was stored as a `public final double crcChance` field on `CompressionParameters`, making it immutable after construction. When a user altered the compression option via `ALTER TABLE`, the new value was recorded in the schema metadata but never propagated to already-open `SSTableReader` instances, so reads continued using the old chance. Additionally, `crc_check_chance` was not registered in the compressor's supported-options set, so specifying it at `CREATE TABLE` time would throw "Unknown compression option".

### Before / After

```java
// BEFORE
public final double crcChance;
// ... read directly from field, not propagatable at runtime
if (metadata.parameters.crcChance > FBUtilities.threadLocalRandom().nextDouble())
```

```java
// AFTER
private volatile double crcCheckChance;
public void setCrcCheckChance(double crcCheckChance) throws ConfigurationException { ... }
public double getCrcCheckChance() { return this.crcCheckChance; }
// CFS iterates open SSTables and calls setCrcCheckChance on each
if (metadata.parameters.getCrcCheckChance() > FBUtilities.threadLocalRandom().nextDouble())
```

---

## How to Spot

When a configuration value that affects open file handles or long-lived objects is stored as a `final` field, any runtime update path will silently have no effect. Look for `public final` primitive fields on objects that are expected to be tuneable at runtime without restart. The fix pattern is `volatile` + getter/setter with propagation to all live instances.

---

## Test

No test added.
