# Bug 0854: nodetool tablestats Omits SSTable Count Line

**Commit**: c9df18c2e2 · **Ticket**: CASSANDRA-12205
**Subsystem**: Tools / nodetool
**Category**: Wrong constant / default value

---

## Root Cause
`TableStatsPrinter` printed per-table statistics but omitted the `SSTable count` line entirely.  The line that prints `table.sstableCount` was simply missing from the output loop; the field was populated correctly by the data-gathering layer but never emitted to the user.  All other table-level statistics (bloom filter info, compaction stats, etc.) were printed correctly.

### Before / After
```java
// BEFORE – sstableCount is collected but never printed
out.println("\t\tTable" + (table.isIndex ? " (index): " : ": ") + table.name);
if (table.isLeveledSstable)
    out.println("\t\tSSTables in each level: [...]");

// AFTER – sstableCount emitted immediately after the table header
out.println("\t\tTable" + (table.isIndex ? " (index): " : ": ") + table.name);
out.println("\t\tSSTable count: " + table.sstableCount);
if (table.isLeveledSstable)
    out.println("\t\tSSTables in each level: [...]");
```

---

## How to Spot
When adding a new statistics field to a data-transfer object, check that every output path (text, JSON, YAML) has a corresponding `print` or serialization call for the new field.  A missing output line is invisible at compile time and easy to miss in review because the field appears to be used (it is referenced in the DTO population code).

---

## Test
No test added.
