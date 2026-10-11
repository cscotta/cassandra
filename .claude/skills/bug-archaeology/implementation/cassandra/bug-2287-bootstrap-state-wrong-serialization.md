# Bug 2287: BootstrapState Stored and Read as Wrong Column Type (boolean vs text / ordinal vs name)

**Commit**: d96e813e06 · **Ticket**: CASSANDRA-4460

## Detection Rule

When an enum is persisted to a database column, verify that both the write path (uses `.name()`) and the read path (uses `valueOf(getString(...))`) match; mismatches occur when the column type or serialization format changes and one side is not updated.

## Code Smell

`BootstrapState.values()[result.one().getInt("bootstrapped")]` reads an integer ordinal from a column declared as `text`, and `setBootstrapState` writes `'%b'` (boolean formatting of the *current* state instead of the *new* state).

## Tags

- scope: cassandra
- category: wrong-serialization
- code-pattern: enum-ordinal-vs-name-mismatch
- severity: crash
- subsystem: bootstrap-startup

## Root Cause

The `bootstrapped` column was changed from `boolean` to `text` to support three states (`NEEDS_BOOTSTRAP`, `COMPLETED`, `IN_PROGRESS`), but the read path still called `getInt("bootstrapped")` and indexed into `BootstrapState.values()` by ordinal. Additionally, `setBootstrapState` incorrectly formatted the *current* state with `%b` (boolean) instead of writing `state.name()` of the *new* state. The fix aligns the schema declaration (`text`), write path (`state.name()`), and read path (`valueOf(getString(...))`) consistently.

## Example

```java
// BEFORE — reads int ordinal from text column; writes boolean of wrong variable
return BootstrapState.values()[result.one().getInt("bootstrapped")];

String req = "INSERT INTO system.%s (key, bootstrapped) VALUES ('%s', '%b')";
processInternal(String.format(req, LOCAL_CF, LOCAL_KEY, getBootstrapState())); // wrong state!
```

```java
// AFTER
return BootstrapState.valueOf(result.one().getString("bootstrapped"));

String req = "INSERT INTO system.%s (key, bootstrapped) VALUES ('%s', '%s')";
processInternal(String.format(req, LOCAL_CF, LOCAL_KEY, state.name()));
```
