# Bug 2629: SerializingCache reads freed native memory due to missing reference counting

**Commit**: d250c4f4d5 · **Ticket**: CASSANDRA-2951

## Detection Rule
When off-heap (native) memory objects stored in a concurrent cache can be evicted on one thread while being read on another, protect every access with reference counting so the memory is not freed while a reader holds a pointer to it.

## Code Smell
Cache `get()` calls `deserialize(mem)` directly on a `FreeableMemory` retrieved from the map, with no reference count acquired first; a concurrent eviction can call `mem.free()` between the map lookup and the deserialize, causing a use-after-free on native memory.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: use-after-free-native-memory
- severity: crash
- subsystem: storage-engine

## Root Cause
`SerializingCache.get()` fetched a `FreeableMemory` from the map and immediately deserialized it. If a concurrent eviction or `remove()` freed the native memory between the map lookup and the read, the deserialization would operate on freed off-heap memory, causing corruption or a JVM crash. The fix introduces an `AtomicInteger` reference count on `FreeableMemory`; `get()` calls `reference()` before reading and `unreference()` after, and eviction/removal calls `unreference()` instead of `free()` directly.

## Example
```java
// BEFORE
FreeableMemory mem = map.get(key);
if (mem == null) return null;
return deserialize(mem);  // use-after-free if evicted concurrently

// AFTER
FreeableMemory mem = map.get(key);
if (mem == null) return null;
if (!mem.reference()) return null;  // bail if already freed
try {
    return deserialize(mem);
} finally {
    mem.unreference();
}
```
