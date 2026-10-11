# Bug 0920: sstabledump Help Text Says "Row key" Instead of "Partition key"

**Commit**: 5a77bd1f11 · **Ticket**: n/a
**Subsystem**: Tools / sstabledump
**Category**: Wrong constant / default value

---

## Root Cause
The CLI option descriptions in `SSTableExport` used the legacy term "Row key" for the `-k`, `-x`, and `-e` options. After the CQL storage model replaced the old wide-row model, the user-facing terminology became "partition key". The help text was never updated, leaving users confused about which key level these flags target.

### Before / After
```java
// BEFORE
Option optKey = new Option(KEY_OPTION, true, "Row key");
Option excludeKey = new Option(EXCLUDE_KEY_OPTION, true, "Excluded row key");
Option optEnumerate = new Option(ENUMERATE_KEYS_OPTION, false, "enumerate keys only");

// AFTER
Option optKey = new Option(KEY_OPTION, true, "Partition key");
Option excludeKey = new Option(EXCLUDE_KEY_OPTION, true, "Excluded partition key");
Option optEnumerate = new Option(ENUMERATE_KEYS_OPTION, false, "enumerate partition keys only");
```

---

## How to Spot
When reviewing tool option registrations, check that the descriptive string passed to `new Option(...)` uses the current storage-model vocabulary. Any occurrence of "Row key" in a CLI help string for a tool that has adopted the CQL partition/clustering model is a stale label. Run `sstabledump --help` and compare its output against the current Cassandra documentation terminology.

---

## Test
No test added.
