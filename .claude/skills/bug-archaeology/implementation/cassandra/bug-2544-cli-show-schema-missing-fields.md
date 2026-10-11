# Bug 2544: CLI `show schema` omits fields and uses reference equality for string comparison

**Commit**: 67b6f83b3b · **Ticket**: CASSANDRA-3304

## Detection Rule
When a `show schema` output loop omits writing fields that exist on the underlying object, and a string conditional uses `==` instead of `.equals()`, the emitted schema is incomplete and the branch never fires.

## Code Smell
A `writeAttr` call is absent for a field that exists in the schema object (e.g., `durable_writes`, `compaction_strategy_options`). A string comparison against a literal uses `==` instead of `.equals()`, silently skipping the branch on non-interned strings.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: reference-equality-string-comparison
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
The `showKeyspace` method never called `writeAttr` for `durable_writes`, so it was silently dropped from the schema output. In `showColumnFamily`, the guard `cfDef.column_type == "Super"` used reference equality (`==`) rather than `.equals()`, so the `subcomparator` field was never emitted for super column families. Additionally, `compaction_strategy_options` was never serialised into the output.

## Example
```java
// BEFORE
if (cfDef.column_type == "Super")
    writeAttr(sb, false, "subcomparator", ...);
// durable_writes write call missing entirely
```
```java
// AFTER
if (cfDef.column_type.equals("Super"))
    writeAttr(sb, false, "subcomparator", ...);
writeAttr(sb, false, "durable_writes", ksDef.durable_writes);
// compaction_strategy_options loop also added
```
