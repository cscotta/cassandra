# Bug 2999: NonBlockingHashMap iteration bug causes duplicate bucket references in compaction bucketing

**Commit**: 50c1987265 · **Ticket**: CASSANDRA-57

## Detection Rule
When a map is modified (remove + re-add) during iteration in a compaction-bucketing loop, prefer `ConcurrentHashMap` over `NonBlockingHashMap` to avoid iterator anomalies that produce duplicate entries.

## Code Smell
A `NonBlockingHashMap` is used as the backing map for compaction buckets; removing a key and re-inserting a new collection during iteration of the same map triggers a known anomaly that results in multiple references to the same bucket collection.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: nonblockingmap-mutation-during-iteration
- severity: silent-wrong-result
- subsystem: compaction

## Root Cause
`getCompactionBuckets()` iterates over a `NonBlockingHashMap` while removing and re-inserting entries in the same iteration. `NonBlockingHashMap` has a known issue where removing and re-adding the same key during its own iteration can expose duplicate references to the same `List` object, causing SSTable files to appear in multiple buckets simultaneously. Switching to `ConcurrentHashMap` (which is safe for concurrent read/remove/add) eliminates the duplicate-bucket problem with negligible performance overhead at this code path.

## Example
```java
// BEFORE: NonBlockingHashMap misbehaves on remove+readd during iteration
Map<List<String>, Long> buckets = new NonBlockingHashMap<List<String>, Long>();

// AFTER: ConcurrentHashMap is safe here
Map<List<String>, Long> buckets = new ConcurrentHashMap<List<String>, Long>();
```
