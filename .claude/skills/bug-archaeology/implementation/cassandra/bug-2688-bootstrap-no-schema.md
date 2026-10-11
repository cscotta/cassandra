# Bug 2688: Bootstrap fails when node has no schema

**Commit**: c8f2414688 · **Ticket**: CASSANDRA-2625

## Detection Rule
When a node skips the bootstrap data transfer (because it has nothing to bootstrap), it must still call `SystemTable.setBootstrapped(true)` and register its token, or it will never join the ring.

## Code Smell
An `if (bootstrap) { bootstrap(token); }` block without a corresponding `else` branch that marks the node as bootstrapped and sets its token, leaving the else-case as a no-op comment.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: missing-else-state-transition
- severity: crash
- subsystem: bootstrap-startup

## Root Cause
When a fresh node had an empty token metadata (no schema) and the bootstrap check determined there was nothing to bootstrap, the code path fell through without calling `SystemTable.setBootstrapped(true)` or `setToken(token)`. The node then never finished joining the ring. The fix adds an `else` branch to handle the no-bootstrap case, marking the node bootstrapped and registering its token so it can participate in the ring.

## Example
```java
// BEFORE
if (needsBootstrap) {
    bootstrap(token);
    assert !isBootstrapMode;
}
// else nothing to do, go directly to participating in ring
```
```java
// AFTER
if (needsBootstrap) {
    bootstrap(token);
    assert !isBootstrapMode;
} else {
    // nothing to bootstrap, go directly to participating in ring
    SystemTable.setBootstrapped(true);
    setToken(token);
}
```
