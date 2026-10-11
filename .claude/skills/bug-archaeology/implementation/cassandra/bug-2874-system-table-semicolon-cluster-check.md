# Bug 2874: SystemTable cluster name check silently skipped due to stray semicolon

**Commit**: ee97297331 · **Ticket**: n/a

## Detection Rule
If an `if` statement's condition is followed by a semicolon before the block, the block becomes an unconditional statement; scan for `if (condition);` patterns that cause mandatory validation to become a no-op.

## Code Smell
`if (!DatabaseDescriptor.getClusterName().equals(new String(clusterCol.value())));` — the trailing semicolon makes the `if` body an empty statement, so the `throw new ConfigurationException(...)` on the next line always executes regardless of whether the cluster name matches.

## Tags
- scope: java
- category: wrong-constant
- code-pattern: semicolon-after-if-condition
- severity: crash
- subsystem: config

## Root Cause
A stray semicolon after the `if` condition turned the cluster-name mismatch guard into an unconditional exception throw on every startup. The fix also adds forced flushing of the system status CF after token update (so the token survives a crash between update and flush) and fixes a separate bug where `incrementAndGetGeneration` was querying the wrong column name (`TOKEN` instead of `GENERATION`).

## Example
```java
// BEFORE — always throws ConfigurationException regardless of cluster name
if (!DatabaseDescriptor.getClusterName().equals(new String(clusterCol.value())));
    throw new ConfigurationException("Saved cluster name ...");
```
```java
// AFTER
if (!DatabaseDescriptor.getClusterName().equals(new String(clusterCol.value())))
    throw new ConfigurationException("Saved cluster name ...");
```
