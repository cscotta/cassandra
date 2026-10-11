# Bug 2432: CommitLog replay uses non-thread-safe HashSet shared across futures

**Commit**: 359391fa83 · **Ticket**: CASSANDRA-3751

## Detection Rule

When a `Set` or other mutable collection is shared across concurrently-executing `Future` or `Runnable` instances without synchronization, replace it with a concurrent collection.

## Code Smell

A `new HashSet<>()` is declared final and shared across a list of `Future<?>` tasks that all mutate it concurrently; no lock or synchronized block guards the set.

## Tags

- scope: cassandra
- category: race-condition
- code-pattern: unsynchronized-shared-collection
- severity: crash
- subsystem: commitlog-cdc

## Root Cause

`CommitLog.recover()` created a plain `HashSet<Table>` and passed it to multiple futures that inserted into it concurrently during replay. `HashSet` is not thread-safe; concurrent modification can corrupt internal state, causing lost entries or exceptions. On systems with many column family definitions the race window was large enough to manifest. The fix replaces it with `NonBlockingHashSet`.

## Example

```java
// BEFORE – not thread-safe
final Set<Table> tablesRecovered = new HashSet<Table>();
List<Future<?>> futures = new ArrayList<Future<?>>();
// ... futures write to tablesRecovered concurrently

// AFTER
final Set<Table> tablesRecovered = new NonBlockingHashSet<Table>();
```
