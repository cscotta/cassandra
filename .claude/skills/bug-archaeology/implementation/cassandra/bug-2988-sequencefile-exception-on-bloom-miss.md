# Bug 2988: SequenceFile throws IOException on bloom filter false positive instead of returning -1

**Commit**: 4a5620dea2 · **Ticket**: CASSANDRA-126

## Detection Rule
When a block-index lookup returns "key not found," return a sentinel value rather than throwing an exception, so callers can treat it as a normal miss.

## Code Smell
`seekTo()` throws `new IOException("This key … does not exist")` when `getPositionFromBlockIndex()` returns -1, turning an expected bloom-filter false positive into an unhandled exception propagated to the caller.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: exception-for-expected-miss
- severity: crash
- subsystem: storage-engine

## Root Cause
Bloom filters intentionally produce false positives: a key that passes the filter may still not exist in the SSTable. `seekTo()` treated the resulting -1 from `getPositionFromBlockIndex()` as an error and threw an IOException instead of treating it as a normal not-found case. This caused reads to fail with an exception whenever a bloom filter false positive occurred. The fix changes `seekTo()` to return the position (which is -1 for not-found) and lets callers short-circuit gracefully.

## Example
```java
// BEFORE: throws on a normal miss
protected void seekTo(String key, Coordinate section) throws IOException {
    seek(section.end_);
    long position = getPositionFromBlockIndex(key);
    if (position == -1)
        throw new IOException("This key " + key + " does not exist.");
    seek(position);
}

// AFTER: returns -1 for not-found; caller handles it
private long seekTo(String key, Coordinate section) throws IOException {
    seek(section.end_);
    long position = getPositionFromBlockIndex(key);
    if (position >= 0)
        seek(position);
    return position;
}
// caller: if (isEOF() || seekTo(key, section) < 0) return bytesRead;
```
