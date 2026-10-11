# Bug 2949: Memtable.isClean() has race condition via volatile isDirty_ flag

**Commit**: 46e5f23e1b · **Ticket**: CASSANDRA-532

## Detection Rule
When a "dirty" flag is set in `put()` and read in `isClean()`, verify whether there is a happens-before guarantee that prevents a flush from treating a just-populated memtable as empty.

## Code Smell
`Memtable.isClean()` returns `!isDirty_` where `isDirty_` is a `volatile boolean` set in `put()`. A flush thread reading `isDirty_ == false` before the write thread sets it can treat a non-empty memtable as clean and skip the flush.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: stale-dirty-flag
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
The `isDirty_` boolean was set eagerly in `put()` but the comment acknowledged that under certain race conditions it could return `false` even after data was written. The fix replaces the volatile flag with `columnFamilies_.isEmpty()`, which reads the actual state of the underlying data structure (a `NonBlockingHashMap`). The same fix was applied to `BinaryMemtable` which never had a `isClean()` method and was calling `submitFlush` on an empty memtable (using `memtable_.isClean()` instead of `binaryMemtable_.get().isClean()`).

## Example
```java
// BEFORE: volatile flag can be stale
private volatile boolean isDirty_;
void put(String key, ColumnFamily cf) { isDirty_ = true; resolve(key, cf); }
public boolean isClean() { return !isDirty_; }
```
```java
// AFTER: check actual map contents
public boolean isClean() { return columnFamilies_.isEmpty(); }
```
