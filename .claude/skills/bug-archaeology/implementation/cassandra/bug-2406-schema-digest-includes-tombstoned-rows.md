# Bug 2406: Schema digest includes deleted/empty CFs causing intermittent SchemaDisagreementException

**Commit**: 9dbb1b7757 · **Ticket**: CASSANDRA-3884

## Detection Rule

When computing a digest or hash for schema agreement, rows that have been logically deleted (tombstoned or marked-for-delete) must be excluded, or different nodes that have different tombstone states will compute different digests even though the effective schema is identical.

## Code Smell

`if (row.cf == null || row.cf.getColumnCount() == 0) continue;` — the guard skips null and empty column families but does not skip ones that are marked-for-delete (tombstoned), so a row in the process of being deleted contributes to the digest on some nodes but not others.

## Tags

- scope: cassandra
- category: wrong-filter-result
- code-pattern: digest-includes-tombstoned-rows
- severity: silent-wrong-result
- subsystem: schema-metadata

## Root Cause

The schema version digest was computed by iterating serialized schema rows and hashing any that had at least one column. A CF that had been dropped was represented by a tombstoned row (`isMarkedForDelete() == true`) that could still pass the `getColumnCount() == 0` check depending on timing, causing different nodes to include or exclude it from the digest and producing false schema disagreements. The fix also skips rows where `cf.isMarkedForDelete()` is true. Additionally, `m.announce()` was being called inside the schema-application `Callable`, causing schema migrations to propagate twice; this double-announce was removed as a separate cleanup in the same fix.

## Example

```java
// BEFORE — tombstoned row included in digest
if (row.cf == null || row.cf.getColumnCount() == 0)
    continue;
row.cf.updateDigest(versionDigest);
```

```java
// AFTER — skip deleted rows too
if (row.cf == null || row.cf.isMarkedForDelete() || row.cf.isEmpty())
    continue;
row.cf.updateDigest(versionDigest);
```
