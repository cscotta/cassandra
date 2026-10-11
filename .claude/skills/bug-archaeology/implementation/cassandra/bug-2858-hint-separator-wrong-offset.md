# Bug 2858: HintedHandOffManager searches for separator at wrong ByteBuffer offset

**Commit**: d5fa915895 · **Ticket**: CASSANDRA-1672

## Detection Rule
When searching backward through a `ByteBuffer`'s backing array for a delimiter, use `limit() + arrayOffset()` as the starting search index, not `position() + arrayOffset()`; searching from position finds only the beginning of the data, missing the separator entirely.

## Code Smell
`ArrayUtils.lastIndexOf(joined.array(), SEPARATOR.getBytes()[0], joined.position()+joined.arrayOffset())` searches backwards from the start of the buffer's data. The separator is located somewhere before the end, so searching from the beginning (position) can never find it correctly — the search proceeds from the wrong anchor.

## Tags
- scope: cassandra
- category: off-by-one
- code-pattern: bytebuffer-wrong-search-anchor
- severity: crash
- subsystem: storage-engine

## Root Cause
`getTableAndCFNames` splits a hint column name (format: `"keyspace:cfname"`) by finding the last separator byte. `ArrayUtils.lastIndexOf` with a `fromIndex` starts the backward search from that index. Using `position()` instead of `limit()` anchored the search at the beginning of the data rather than the end, so the separator was never found and a `RuntimeException("Corrupted hint name")` was thrown for every valid hint delivery.

## Example
```java
// BEFORE
index = ArrayUtils.lastIndexOf(joined.array(), SEPARATOR.getBytes()[0],
    joined.position()+joined.arrayOffset());
if (index < 1)
    throw new RuntimeException("Corrupted hint name " + joined.toString());

// AFTER
index = ArrayUtils.lastIndexOf(joined.array(), SEPARATOR.getBytes()[0],
    joined.limit()+joined.arrayOffset());
if (index == -1 || index < (joined.position() + joined.arrayOffset() + 1))
    throw new RuntimeException("Corrupted hint name ...");
```
