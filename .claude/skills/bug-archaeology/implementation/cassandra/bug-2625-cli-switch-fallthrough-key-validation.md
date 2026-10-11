# Bug 2625: Missing break in CLI switch causes fallthrough from KEY_VALIDATION_CLASS to COMPACTION_STRATEGY

**Commit**: b53554f5c3 · **Ticket**: n/a

## Detection Rule
When a switch statement handles enum cases with side-effecting method calls, verify every case ends with a `break` or `return` to prevent unintended fallthrough to the next case.

## Code Smell
A `case KEY_VALIDATION_CLASS:` block that sets `cfDef.setKey_validation_class(...)` has no `break;`, so execution continues into `case COMPACTION_STRATEGY:` and overwrites the compaction strategy with the key-validation value.

## Tags
- scope: java
- category: wrong-filter-result
- code-pattern: missing-switch-break
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
In `CliClient`, the `KEY_VALIDATION_CLASS` case in the column family attribute update switch block was missing a `break` statement. After setting the key validation class, execution fell through and also set `compaction_strategy` to the same string value, silently corrupting the CF definition whenever a user specified `key_validation_class` in the CLI.

## Example
```java
// BEFORE
case KEY_VALIDATION_CLASS:
    cfDef.setKey_validation_class(CliUtils.unescapeSQLString(mValue));
case COMPACTION_STRATEGY:                // ← fallthrough corrupts compaction_strategy
    cfDef.setCompaction_strategy(CliUtils.unescapeSQLString(mValue));
    break;

// AFTER
case KEY_VALIDATION_CLASS:
    cfDef.setKey_validation_class(CliUtils.unescapeSQLString(mValue));
    break;
case COMPACTION_STRATEGY:
    cfDef.setCompaction_strategy(CliUtils.unescapeSQLString(mValue));
    break;
```
