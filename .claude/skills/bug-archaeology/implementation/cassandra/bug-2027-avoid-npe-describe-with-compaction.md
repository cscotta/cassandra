# Bug 2027: NPE in CLI When Describing a Column Family with No Compaction Strategy Options

**Commit**: d349787515 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cli/CliClient.java
**Category**: Missing null / bounds check

---

## Root Cause

`CliClient` called `cf_def.compaction_strategy_options.isEmpty()` without first verifying that the field was non-null. When a column family definition was serialized with no compaction strategy options the field could arrive as `null` from Thrift, causing a `NullPointerException` when the `describe cf` command was run on such a column family.

### Before / After

```java
// BEFORE
if (!cf_def.compaction_strategy_options.isEmpty())
```

```java
// AFTER
if (cf_def.compaction_strategy_options != null && !cf_def.compaction_strategy_options.isEmpty())
```

---

## How to Spot

Any access to a Thrift-provided map or collection field should be preceded by a null check. Thrift optional fields default to `null`, not to empty collections, so `isEmpty()` on an unset Thrift field will always throw.

---

## Test

No test added.
