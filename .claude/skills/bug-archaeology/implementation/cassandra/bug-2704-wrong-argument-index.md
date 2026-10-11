# Bug 2704: Wrong argument index causes nodetool setcompactionthroughput to always fail

**Commit**: 4be0b109f0 · **Ticket**: CASSANDRA-2550

## Detection Rule
When a CLI command reads arguments by position, verify that the argument array index matches the command's own argument count check.

## Code Smell
A length check compares against `2` but the actual value is read from `arguments[1]`; the subcommand strips the command name so the remaining array is off by one.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: off-by-one-argument-index
- severity: crash
- subsystem: tools-clients

## Root Cause
The `arguments` array passed to the handler already has the verb token consumed, so it contains only the user-supplied parameters starting at index 0. The length check `!= 2` should be `!= 1` and the value should be read from `arguments[0]`, not `arguments[1]`. With the original code the guard never triggers for a valid single argument, and `arguments[1]` is always an `ArrayIndexOutOfBoundsException`.

## Example
```java
// BEFORE
case SETCOMPACTIONTHROUGHPUT :
    if (arguments.length != 2) { badUse("Missing value argument."); }
    probe.setCompactionThroughput(Integer.valueOf(arguments[1]));
    break;
```
```java
// AFTER
case SETCOMPACTIONTHROUGHPUT :
    if (arguments.length != 1) { badUse("Missing value argument."); }
    probe.setCompactionThroughput(Integer.valueOf(arguments[0]));
    break;
```
