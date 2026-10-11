# Bug 002: Pre-2.1 Snapshot Directories Invisible to SnapshotLoader

**Commit**: 989901e4e0 · **Ticket**: CASSANDRA-21173
**Subsystem**: `org.apache.cassandra.service.snapshot` — `nodetool listsnapshots` / `clearsnapshot`
**Category**: New component re-implements format parsing without covering legacy format; silent skip on non-match

---

## Root Cause

`SnapshotLoader` used a regex with a required `tableId` group. Pre-2.1 directories have no UUID suffix — they silently failed to match and were skipped entirely.

### Before / After

```java
// SnapshotLoader.java BEFORE — tableId required, pre-2.1 paths never match
Pattern.compile("(?<keyspace>\\w+)/(?<tableName>\\w+)" +
                "-(?<tableId>[0-9a-f]{32})" +   // required — no match = silent skip
                "/snapshots/(?<tag>.+)$");
```

```java
// SnapshotLoader.java AFTER — tableId optional
Pattern.compile("(?<keyspace>\\w+)/(?<tableName>\\w+)" +
                "(-(?<tableId>[0-9a-f]{32}))?" + // optional — pre-2.1 paths now match
                "/snapshots/(?<tag>.+)$");
```

```java
// TableSnapshot.java AFTER — null-safe buildSnapshotId
String.format("%s:%s:%s:%s", ks, table, tableId == null ? "" : tableId, tag);
```

`Directories.java` had documented this backward-compat rule in its Javadoc all along; `SnapshotLoader` (a post-4.0 class) simply never consulted it.

---

## How to Spot

Anywhere a regex is applied to a filesystem path: check whether every historical format is covered (or explicitly rejected with a warning). Silent-skip (`if (matcher.find()) { ... }` with no `else` warn) on filesystem data makes missing matches invisible. New components in a long-lived codebase often re-implement path parsing without inheriting existing backward-compat logic.

---

## Test

`SnapshotLoaderTest.testSnapshotsWithManifests`: one table's snapshot dir created as `TABLE2_NAME` (no UUID suffix). Asserts snapshot is found and has `tableId == null`.
