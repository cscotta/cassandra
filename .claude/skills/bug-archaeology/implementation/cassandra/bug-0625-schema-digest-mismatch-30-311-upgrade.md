# Bug 0625: Schema Digest Mismatch Triggers Continuous Schema Pull During 3.0→3.11 Rolling Upgrade

**Commit**: e646e5032b · **Ticket**: CASSANDRA-14109
**Subsystem**: `src/java/org/apache/cassandra/config/Schema.java`, `org/apache/cassandra/schema/SchemaKeyspace.java`, `org/apache/cassandra/service/MigrationManager.java`, `org/apache/cassandra/gms/Gossiper.java`
**Category**: Wrong serialization / deserialization

---

## Root Cause

Cassandra 3.11 added a `cdc` column to several system schema tables. When computing the schema digest (an MD5 over all schema rows used as a version UUID), 3.11 nodes include this column in the digest while 3.0 nodes do not. During a rolling upgrade, every 3.0 node therefore sees a different schema version from every 3.11 node and schedules a `MigrationTask` to pull the schema, which then lands and triggers the same mismatch in reverse, creating a continuous, never-converging schema exchange loop. The fix makes 3.11 compute two digests simultaneously — a full one (the "real" 3.11 digest) and one that excludes the `cdc` column (a 3.0-compatible digest) — and gossips whichever matches what the peer cluster needs, based on whether any 3.0 node is still present. `MigrationManager.maybeScheduleSchemaPull` was also updated to compare the remote version against both local digests before deciding to pull.

### Before / After

```java
// BEFORE – single digest, always includes cdc column
public static UUID calculateSchemaDigest() {
    MessageDigest digest = MessageDigest.getInstance("MD5");
    // ... digests all rows including cdc column ...
    return UUID.nameUUIDFromBytes(digest.digest());
}
```

```java
// AFTER – dual digest: full (3.11) and 3.0-compatible
public static Pair<UUID, UUID> calculateSchemaDigest() {
    MessageDigest digest   = MessageDigest.getInstance("MD5");
    MessageDigest digest30 = MessageDigest.getInstance("MD5");
    Set<ByteBuffer> cdc = Collections.singleton(ByteBufferUtil.bytes("cdc"));
    // ... digest30 skips the cdc column via columnsToExclude ...
    return Pair.create(
        UUID.nameUUIDFromBytes(digest.digest()),
        UUID.nameUUIDFromBytes(digest30.digest()));
}
```

---

## How to Spot

When two Cassandra versions add or remove schema-metadata columns, any code that computes a schema digest must be aware that peers on older versions will produce a structurally different digest. Reviewers should check that schema-version comparison logic accounts for all announced versions a mixed-version cluster could produce, not just the local node's single computed version. The presence of a hard equality check (`version.equals(theirVersion)`) against a single UUID is a red flag during any schema-format migration.

---

## Test

No unit test added in this commit; the change is validated through integration testing of rolling upgrades from 3.0 to 3.11.
