# Bug 2514: CompressionMetadata cache leaks stale entries and blocks re-creation after compaction

**Commit**: 981ee774eb · **Ticket**: CASSANDRA-3427

## Detection Rule
When a static `ConcurrentHashMap` caches objects keyed by file path and those files are replaced by compaction, verify that entries are invalidated on SSTable replacement; a perpetually growing cache that is never invalidated causes stale reads and memory leaks.

## Code Smell
`private final static Map<String, CompressionMetadata> cache = new ConcurrentHashMap<>()` with `cache.put(dataFilePath, metadata)` but no corresponding `cache.remove()` — after compaction replaces an SSTable, the old `CompressionMetadata` (pointing to a deleted file) lives in the cache indefinitely, and the new file's metadata is never created.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: unbounded-static-cache-no-eviction
- severity: crash
- subsystem: storage-engine

## Root Cause
The static cache was intended to avoid creating multiple `CompressionMetadata` objects per SSTable, but had no eviction mechanism. After compaction deleted an SSTable and created a replacement with the same path, the cache returned the old metadata object (whose offsets index pointed to the previous file), leading to incorrect reads. The fix removes the cache entirely and instead stores `CompressionMetadata` as a field in `SSTableReader` (constructed once, at open time), so lifecycle is tied to the reader's lifetime.

## Example
```java
// BEFORE — static cache, never evicted
public static CompressionMetadata get(String dataFilePath) {
    CompressionMetadata metadata = cache.get(dataFilePath);
    if (metadata != null)
        return metadata;
    metadata = new CompressionMetadata(...);
    cache.put(dataFilePath, metadata);
    return metadata;
}
```
```java
// AFTER — create once per SSTableReader, no cache
public static CompressionMetadata create(String dataFilePath) {
    return new CompressionMetadata(...);
}
```
