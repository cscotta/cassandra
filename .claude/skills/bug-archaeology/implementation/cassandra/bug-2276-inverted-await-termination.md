# Bug 2276: Inverted awaitTermination Condition Silences Stream Timeout Errors

**Commit**: afb2c00e58 · **Ticket**: CASSANDRA-4484

## Detection Rule

When `ExecutorService.awaitTermination()` is called and the result drives an error log, confirm the condition tests `!awaitTermination(...)` (timed out = false return) rather than `awaitTermination(...)` (completed = true return).

## Code Smell

`if (e.awaitTermination(...))` logs the error when the executor *did* finish within the timeout — the opposite of the intended behavior; the log should fire when it *did not* finish (i.e., returned `false`).

## Tags

- scope: java
- category: logic-error
- code-pattern: inverted-boolean-condition
- severity: silent-wrong-result
- subsystem: network-messaging

## Root Cause

`ExecutorService.awaitTermination()` returns `true` if the executor terminated within the timeout and `false` if it timed out. The condition `if (e.awaitTermination(24, TimeUnit.HOURS))` triggered the "took more than 24H" error message on successful completion and silently swallowed the message on actual timeout — exactly backwards. The fix adds `!` to invert the condition.

## Example

```java
// BEFORE
if (e.awaitTermination(24, TimeUnit.HOURS))
    logger.error("Stream took more than 24H to complete; skipping");
// fires on SUCCESS, silent on TIMEOUT
```

```java
// AFTER
if (!e.awaitTermination(24, TimeUnit.HOURS))
    logger.error("Stream took more than 24H to complete; skipping");
// fires on TIMEOUT, silent on success
```
