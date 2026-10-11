# Bug 1282: Nodetool `cfstats` and `cfhistograms` Commands Not Registered Under New Table-Oriented Names

**Commit**: bf51f24a6f · **Ticket**: CASSANDRA-8872
**Subsystem**: src/java/org/apache/cassandra/tools/nodetool/
**Category**: Wrong constant / default value

---

## Root Cause

When the nodetool subcommands `cfstats` and `cfhistograms` were renamed to `tablestats` and `tablehistograms` to align with the CQL3 "table" terminology, the new `TableStats` and `TableHistograms` classes were created but never registered in the `NodeTool` command list. Only the old `CfStats`/`CfHistograms` entries were present, so the preferred new names were silently absent from the CLI. The fix both registers the new commands and converts the old classes to deprecated empty subclasses that delegate to the new ones, preserving backwards compatibility.

### Before / After

```java
// BEFORE — NodeTool command list only contained legacy names
Ring.class,
NetStats.class,
CfStats.class,
CfHistograms.class,
```

```java
// AFTER — new preferred names added; old classes become deprecated stubs
Ring.class,
NetStats.class,
CfStats.class,
TableStats.class,
CfHistograms.class,
TableHistograms.class,
```

---

## How to Spot

When a command or class is renamed by introducing a new class that extends or replaces the old one, check that every registration list, factory map, or plugin manifest that referenced the old name is updated to also include (or replace with) the new name. A missing registration leaves the new name inaccessible at runtime while the old name continues to work, making the rename invisible to users.

---

## Test

No test added.
