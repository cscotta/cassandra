# Bug 2985: Memtable added to pending-flush set after being replaced as active, losing writes

**Commit**: 41ee512d6f · **Ticket**: CASSANDRA-161

## Detection Rule
When replacing an active memtable, add it to the pending-flush set before installing the replacement, so no writes land in a memtable that is neither active nor pending-flush.

## Code Smell
A two-step publish sequence (add to pending set, then swap active reference) is done in reverse order: the memtable is swapped out first, creating a window where it is neither active nor in the pending-flush set, making its data invisible to reads.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: swap-before-register
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`switchMemtable()` called `memtable_.set(new Memtable(...))` before adding the old memtable to `memtablesPendingFlush`. In the gap between these two operations a reader would not find the old memtable in either the active slot or the pending-flush set, causing recently written data to be invisible. The fix adds the old memtable to the pending-flush set first, then swaps in the new one.

## Example
```java
// BEFORE: swap happens before registration — data invisible in the gap
memtable_.set(new Memtable(table_, columnFamily_));
// ...later...
getMemtablesPendingFlushNotNull(columnFamily_).add(oldMemtable);

// AFTER: register first, then swap
getMemtablesPendingFlushNotNull(columnFamily_).add(memtable_.get()); // register first
memtable_.set(new Memtable(table_, columnFamily_));
```
