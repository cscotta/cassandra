# Bug 2778: Stress Tool Reads Wrong Option Value for Cardinality

**Commit**: 3ae66eea88 · **Ticket**: CASSANDRA-2121

## Detection Rule
When parsing a command-line option, pass the same option letter to `getOptionValue` that was checked with `hasOption`; using a different letter silently reads the wrong option's value.

## Code Smell
`cmd.getOptionValue("t")` is called inside the `if (cmd.hasOption("C"))` block; `"t"` and `"C"` are different option keys, so the cardinality value is always read from the wrong option.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: wrong-option-key-in-getvalue
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
`Session.parseOptions` checked `cmd.hasOption("C")` but then called `cmd.getOptionValue("t")` to read the cardinality. The option key `"t"` corresponds to the number-of-threads option, not cardinality. If `-C` was passed, the cardinality was silently set to the thread count value (or caused a parse error). The fix changes `getOptionValue("t")` to `getOptionValue("C")`.

## Example
```java
// BEFORE
if (cmd.hasOption("C"))
    cardinality = Integer.parseInt(cmd.getOptionValue("t")); // wrong key
```
```java
// AFTER
if (cmd.hasOption("C"))
    cardinality = Integer.parseInt(cmd.getOptionValue("C")); // correct key
```
