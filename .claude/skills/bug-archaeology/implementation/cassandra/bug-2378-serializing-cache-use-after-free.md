# Bug 2378: SerializingCache replace() deserialises freed off-heap memory, causing segfault

**Commit**: 7e3bb8644d · **Ticket**: CASSANDRA-4111

## Detection Rule

When performing a compare-and-swap on off-heap (mmap) memory, verify that the old value's reference count is incremented before deserialising it — if the memory is freed by another thread between the CAS attempt and the deserialisation, accessing freed native memory produces a JVM segfault.

## Code Smell

`V oldValue = deserialize(old)` is called on a `FreeableMemory` object without first calling `old.reference()`, so another thread may call `old.unreference()` between the null check and the deserialise, freeing the native buffer while it is being read.

## Tags

- scope: cassandra
- category: race-condition
- code-pattern: use-after-free-off-heap
- severity: crash
- subsystem: storage-engine

## Root Cause

`SerializingCache.replace(K, V, V)` deserialised the existing cache entry (`old`) to compare it with `oldToReplace` as part of a compare-and-swap operation. If another thread concurrently evicted `old` and called `old.unreference()` (reducing the ref-count to 0 and freeing the native buffer) between the CAS start and the `deserialize(old)` call, the deserialisation would read freed native memory, causing a JVM segfault. The fix calls `old.reference()` to increment the ref-count before deserialising, wraps the deserialise in try/finally to guarantee `unreference()`, and returns `false` early if the object was already freed (`reference()` returns `false`).

## Example

```java
// BEFORE
V oldValue = deserialize(old); // old may be freed concurrently — segfault risk
boolean success = oldValue.equals(oldToReplace) && map.replace(key, old, mem);
if (success)
    old.unreference();
```

```java
// AFTER
if (!old.reference())
    return false; // already freed
try {
    oldValue = deserialize(old);
} finally {
    old.unreference();
}
boolean success = oldValue.equals(oldToReplace) && map.replace(key, old, mem);
if (success)
    old.unreference(); // final release after successful swap
```
