# Bug 2897: CLI read_repair_chance sets key_cache_size instead; gc_grace_seconds has no enum entry

**Commit**: 27c72dd66e · **Ticket**: CASSANDRA-1399

## Detection Rule
When adding a new CLI column family property, verify that both the enum entry and the corresponding switch case call the correct setter method — a copy-paste from an adjacent property can silently set the wrong field.

## Code Smell
`case READ_REPAIR_CHANCE:` calls `cfDef.setKey_cache_size(Double.parseDouble(...))` — a copy-paste error from the `KEY_CACHE_SIZE` case. Additionally `GC_GRACE_SECONDS` was missing from the `ColumnFamilyArgument` enum entirely, so it could never be parsed.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: copy-paste-wrong-setter
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
Two independent errors: (1) `READ_REPAIR_CHANCE` case called `setKey_cache_size` instead of `setRead_repair_chance` — the correct value was discarded and key_cache_size was set to the read_repair_chance value. (2) `GC_GRACE_SECONDS` was absent from the `ColumnFamilyArgument` enum so any attempt to set it via the CLI threw an `IllegalArgumentException` before reaching the switch.

## Example
```java
// BEFORE — wrong setter called for READ_REPAIR_CHANCE
case READ_REPAIR_CHANCE:
    cfDef.setKey_cache_size(Double.parseDouble(CliUtils.unescapeSQLString(mValue)));
    break;
```
```java
// AFTER
case READ_REPAIR_CHANCE:
    cfDef.setRead_repair_chance(Double.parseDouble(mValue));
    break;
case GC_GRACE_SECONDS:
    cfDef.setGc_grace_seconds(Integer.parseInt(mValue));
    // (break added in 2876)
```
