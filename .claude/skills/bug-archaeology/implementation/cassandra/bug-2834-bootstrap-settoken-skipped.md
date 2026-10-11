# Bug 2834: Bootstrap regression: setToken skipped when already bootstrapped

**Commit**: 1a37ebf553 · **Ticket**: n/a (regression from CASSANDRA-1829)

## Detection Rule
When refactoring bootstrap flow, ensure that `setToken` is called unconditionally for all paths that reach normal ring participation, not gated on a boolean that only one of the two bootstrap paths sets.

## Code Smell
`if (!bootstrapped) setToken(token)` where `bootstrapped` is only set to `true` inside the new-bootstrap branch. A node that had already bootstrapped previously but restarts would have `bootstrapped = false`, yet would skip calling `setToken` if the autoBootstrap condition is not met — or `setToken` could be skipped due to incorrect branching logic.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: conditional-token-registration
- severity: crash
- subsystem: bootstrap-startup

## Root Cause
A previous refactor introduced a `bootstrapped` flag to guard `setToken` and `SystemTable.setBootstrapped(true)`, but placed `SystemTable.setBootstrapped(true)` inside the inner bootstrap block. Nodes that restart after having already bootstrapped never re-set their token in the ring. The fix removes the flag and calls `SystemTable.setBootstrapped(true)` and `setToken(token)` unconditionally after all bootstrap-or-skip branching.

## Example
```java
// BEFORE
boolean bootstrapped = false;
if (autoBootstrap && ...) {
    bootstrap(token);
    bootstrapped = true;
    SystemTable.setBootstrapped(true);
}
if (!bootstrapped)
    setToken(token);

// AFTER
if (autoBootstrap && ...) {
    bootstrap(token);
}
SystemTable.setBootstrapped(true);
setToken(token);
```
