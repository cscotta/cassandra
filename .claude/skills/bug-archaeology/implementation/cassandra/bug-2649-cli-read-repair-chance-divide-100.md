# Bug 2649: CLI read_repair_chance divided by 100 before storing, causing values 100x smaller than specified

**Commit**: 93aaeba8af · **Ticket**: CASSANDRA-2837

## Detection Rule
When parsing a CLI attribute that is already expected as a fraction (0.0–1.0), do not divide the user-supplied value by 100; only validate that the result does not exceed 1.0.

## Code Smell
`CliClient` parses `read_repair_chance` as `Double.parseDouble(mValue) / 100`, which divides a value like `0.5` by 100 to get `0.005`, silently setting a much lower repair probability than the user intended; the validation message also misleadingly describes the threshold as divided-by-100.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: spurious-divide-by-100
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
The CLI `READ_REPAIR_CHANCE` case applied a `/100` division to the parsed double value before passing it to `cfDef.setRead_repair_chance()`. Since `read_repair_chance` is already a probability in the range [0.0, 1.0], the division was incorrect — a user specifying `0.5` (50% chance) would have `0.005` (0.5% chance) silently stored. The fix removes the division and corrects the error message to no longer mention `/ 100`.

## Example
```java
// BEFORE
double chance = Double.parseDouble(mValue) / 100;  // 0.5 becomes 0.005
if (chance > 1)
    throw new RuntimeException("Error: read_repair_chance / 100 should not be greater than 1.");

// AFTER
double chance = Double.parseDouble(mValue);
if (chance > 1)
    throw new RuntimeException("Error: read_repair_chance should not be greater than 1.");
```
