# Bug 2752: NPE in nodetool When Gossip Is Not Yet Initialized

**Commit**: c75b0c1d2b · **Ticket**: CASSANDRA-2270

## Detection Rule
When an MBean method may return null or throw if the subsystem is not yet initialized, guard the call with an initialization check before invoking it.

## Code Smell
`probe.getCurrentGenerationNumber()` is called unconditionally inside `printInfo`; if gossip has not started yet this call throws an NPE because the generation number is not available.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: uninitialised-subsystem-access
- severity: crash
- subsystem: gossip

## Root Cause
`NodeCmd.printInfo` always called `probe.getCurrentGenerationNumber()` even when `probe.isInitialized()` returned false, causing an NPE before gossip starts. The fix caches `gossipInitialized` and only calls `getCurrentGenerationNumber()` when gossip is active, printing `0` otherwise.

## Example
```java
// BEFORE
outs.printf("%-17s: %s%n", "Gossip active", probe.isInitialized());
outs.printf("%-17s: %s%n", "Generation No", probe.getCurrentGenerationNumber());
```
```java
// AFTER
boolean gossipInitialized = probe.isInitialized();
outs.printf("%-17s: %s%n", "Gossip active", gossipInitialized);
if (gossipInitialized)
    outs.printf("%-17s: %s%n", "Generation No", probe.getCurrentGenerationNumber());
else
    outs.printf("%-17s: %s%n", "Generation No", 0);
```
