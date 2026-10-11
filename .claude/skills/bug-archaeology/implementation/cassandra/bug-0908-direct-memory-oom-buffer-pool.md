# Bug 0908: Direct Memory OOM Crashes Buffer Pool Allocation

**Commit**: 31cab36b18 · **Ticket**: CASSANDRA-11710
**Subsystem**: BufferPool / memory management
**Category**: Missing null / bounds check

---

## Root Cause
`BufferPool` allocates large off-heap "macro chunks" via `allocateDirectAligned(MACRO_CHUNK_SIZE)`. If the JVM's direct-memory limit (`-XX:MaxDirectMemorySize`) was too small, or if prior allocations had already exhausted it, this call threw `OutOfMemoryError` and propagated uncaught, crashing the thread and causing the allocation path to fail hard with no fallback. The fix wraps the allocation in a try/catch for `OutOfMemoryError`, logs a clear actionable warning, and returns `false` so the caller falls back to on-heap buffers. Additionally, the `file_cache_size_in_mb` config default was changed from a hardcoded `512` to `null`, with `DatabaseDescriptor.getFileCacheSizeInMB()` returning `0` in client mode where no config is loaded — preventing an inadvertent 512 MB direct allocation in tools that do not need it.

### Before / After
```java
// BEFORE — OOM propagates uncaught
Chunk chunk = new Chunk(allocateDirectAligned(MACRO_CHUNK_SIZE));

// AFTER — graceful fallback
Chunk chunk;
try {
    chunk = new Chunk(allocateDirectAligned(MACRO_CHUNK_SIZE));
} catch (OutOfMemoryError oom) {
    noSpamLogger.error("Buffer pool failed to allocate chunk ... buffers will be allocated in on-heap memory ...", ...);
    return false;
}
```

---

## How to Spot
Direct-memory allocations (`ByteBuffer.allocateDirect`, `Unsafe.allocateMemory`, aligned variants) can throw `OutOfMemoryError` independently of heap exhaustion, but they are often written as simple assignments with no error handling. In review, look for direct-allocation call sites that are not inside a try/catch for `OutOfMemoryError` and ask whether a fallback path exists. Also watch for hardcoded capacity defaults that are summed at startup — if the total exceeds `MaxDirectMemorySize`, the first allocation to hit the limit will blow up.

---

## Test
No test added.
